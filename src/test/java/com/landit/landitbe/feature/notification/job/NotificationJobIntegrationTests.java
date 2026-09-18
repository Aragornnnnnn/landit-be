// 체험 예약과 이메일 테스트의 권한, 멱등성 및 채널별 발송 결과를 검증한다.

package com.landit.landitbe.feature.notification.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.landit.landitbe.config.notification.TrialReminderProperties;
import com.landit.landitbe.feature.notification.delivery.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.delivery.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.email.client.EmailSendResult;
import com.landit.landitbe.feature.notification.email.client.EmailSendResult.Status;
import com.landit.landitbe.feature.notification.email.client.EmailSender;
import com.landit.landitbe.feature.notification.email.service.NotificationEmailTemplateService;
import com.landit.landitbe.feature.notification.job.admin.service.AdminNotificationJobService;
import com.landit.landitbe.feature.notification.job.dto.NotificationJob;
import com.landit.landitbe.feature.notification.job.dto.TrialReminderSettings;
import com.landit.landitbe.feature.notification.job.messaging.NotificationJobScheduler;
import com.landit.landitbe.feature.notification.job.service.NotificationJobProcessingService;
import com.landit.landitbe.feature.notification.job.service.NotificationJobReservationService;
import com.landit.landitbe.feature.notification.job.service.NotificationJobService;
import com.landit.landitbe.feature.profile.subscription.service.ProfileSubscriptionService;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 실제 DB와 보안 필터를 통과하고 외부 발송만 대체한다. */
@SpringBootTest(
    properties = {
      "landit.notification.trial-reminder.annual-product-ids=annual",
      "landit.subscription.revenuecat.webhook-authorization=test-reminder-secret"
    })
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class NotificationJobIntegrationTests {
  private static final long USER_ID = 99505001L;
  private static final Instant NOW = Instant.parse("2026-09-16T00:00:00Z");
  @Autowired private NotificationJobService jobs;
  @Autowired private AdminNotificationJobService adminJobs;
  @Autowired private ProfileSubscriptionService profiles;
  @Autowired private TrialReminderProperties policy;
  @Autowired private Validator validator;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManager entityManager;
  @Autowired private MockMvc mvc;
  @MockitoBean private Clock clock;
  private EmailSender sender;
  private NotificationDispatchService push;
  private NotificationJobProcessingService processor;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(NOW);
    when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    ReflectionTestUtils.setField(adminJobs, "consumerEnabled", true);
    sender = mock(EmailSender.class);
    push = mock(NotificationDispatchService.class);
    processor =
        new NotificationJobProcessingService(
            jobs,
            profiles,
            push,
            sender,
            new NotificationEmailTemplateService(),
            policy,
            clock,
            validator);
    jdbc.update(
        """
        INSERT INTO user_profile (id, nickname, email, target_locale, base_locale, current_level,
          push_permission_status, status, role, created_at, updated_at,
          subscription_status, subscription_period_type, subscription_product_id,
          subscription_store, subscription_expires_at)
        VALUES (?, 'reminder-test', 'member@example.com', 'EN', 'KR', 1, 'DENIED', 'ACTIVE',
          'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE', 'TRIAL', 'annual', 'APP_STORE', ?)
        """,
        USER_ID,
        LocalDateTime.ofInstant(NOW.plusSeconds(86400), ZoneId.of("Asia/Seoul")));
  }

  @DisplayName("채널이 꺼져 있어도 채널별 기한에 만료 예약을 종료한다.")
  @Test
  void expiredReservationsCloseAtEachChannelDeadlineEvenWhenDisabled() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    final var test = adminJobs.requestTest(USER_ID, UUID.randomUUID(), "test@example.com");
    final NotificationJob mail = job("TRIAL_EMAIL");
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(false, false));
    when(clock.instant()).thenReturn(NOW.plusSeconds(3600));
    assertThat(jobs.pendingReservations()).isEmpty();
    assertThat(jobs.find(test.id()).orElseThrow().resultCode()).isEqualTo("TOO_LATE");
    assertThat(jobs.find(mail.id()).orElseThrow().status()).isEqualTo("PENDING");
    when(clock.instant()).thenReturn(NOW.plusSeconds(7200));
    assertThat(jobs.reserve(mail.id())).isFalse();
    assertThat(jobs.pendingReservations()).isEmpty();
    assertThat(jobs.find(mail.id()).orElseThrow().resultCode()).isEqualTo("TOO_LATE");
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().status()).isEqualTo("SKIPPED");
  }

  @DisplayName("영구적인 예약 실패는 10회 시도 후 중단한다.")
  @Test
  void permanentReservationFailureStopsAtTenAttempts() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    NotificationJobScheduler scheduler = mock(NotificationJobScheduler.class);
    doThrow(new IllegalStateException("unavailable"))
        .when(scheduler)
        .schedule(org.mockito.ArgumentMatchers.any());
    var reservations = new NotificationJobReservationService(jobs, scheduler);
    for (int minute = 0; minute <= 11; minute++) {
      when(clock.instant()).thenReturn(NOW.plusSeconds(minute * 60L));
      reservations.registerPending();
    }
    verify(scheduler, times(20)).schedule(org.mockito.ArgumentMatchers.any());
    assertThat(jobs.find(job("TRIAL_EMAIL").id()).orElseThrow().status()).isEqualTo("FAILED");
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().resultCode())
        .isEqualTo("SCHEDULE_RETRIES_EXHAUSTED");
    assertThat(jobs.pendingReservations()).isEmpty();
  }

  @DisplayName("일시적인 예약 실패는 선점 만료 후 재시도하고 성공 상태는 보존한다.")
  @Test
  void temporaryReservationFailureRetriesAfterLeaseAndPreservesSuccess() {
    adminJobs.requestTest(USER_ID, UUID.randomUUID(), "test@example.com");
    NotificationJobScheduler scheduler = mock(NotificationJobScheduler.class);
    doThrow(new IllegalStateException("temporary"))
        .doNothing()
        .when(scheduler)
        .schedule(org.mockito.ArgumentMatchers.any());
    var reservations = new NotificationJobReservationService(jobs, scheduler);
    reservations.registerPending();
    reservations.registerPending();
    verify(scheduler).schedule(org.mockito.ArgumentMatchers.any());
    when(clock.instant()).thenReturn(NOW.plusSeconds(60));
    reservations.registerPending();
    when(clock.instant()).thenReturn(NOW.plusSeconds(120));
    reservations.registerPending();
    verify(scheduler, times(2)).schedule(org.mockito.ArgumentMatchers.any());
    assertThat(jobs.pendingReservations()).isEmpty();
    assertThat(jobs.find(job("TEST_EMAIL").id()).orElseThrow().status()).isEqualTo("PENDING");
  }

  @DisplayName("체험 알림 채널은 활성 상태로 시작하고 DB 기본값도 활성이다.")
  @Test
  void trialChannelsStartEnabledAndUseEnabledColumnDefaults() {
    assertThat(jobs.settings()).isEqualTo(new TrialReminderSettings(true, true));
    jdbc.update("DELETE FROM trial_reminder_settings WHERE id = 1");
    jdbc.update("INSERT INTO trial_reminder_settings (id) VALUES (1)");
    assertThat(jobs.settings()).isEqualTo(new TrialReminderSettings(true, true));
  }

  @DisplayName("푸시를 거부한 사용자에게도 이메일은 한 번만 보낸다.")
  @Test
  void deniedPushStillSendsEmailOnlyOnce() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    jobs.recordTrial(USER_ID, "PRODUCTION");
    assertThat(jobs.pendingReservations()).hasSize(2);
    when(sender.send(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new EmailSendResult(Status.ACCEPTED, "ses-1"));
    NotificationJob mail = job("TRIAL_EMAIL");
    processor.process(job("TRIAL_PUSH").id());
    processor.process(mail.id());
    processor.process(mail.id());
    assertThat(jobs.find(mail.id()).orElseThrow().providerMessageId()).isEqualTo("ses-1");
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().resultCode())
        .isEqualTo("PUSH_PERMISSION_DENIED");
    verify(sender, times(1))
        .send(
            eq("member@example.com"),
            anyString(),
            contains("발신 전용"),
            contains("cid:landit-banner"));
    verifyNoInteractions(push);
  }

  @DisplayName("구독 취소나 채널 비활성화 시 대기 작업을 제외한다.")
  @Test
  void cancellationAndChannelOffExcludePendingJobs() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    NotificationJob mail = job("TRIAL_EMAIL");
    jdbc.update("UPDATE user_profile SET subscription_status = 'CANCELED' WHERE id = ?", USER_ID);
    entityManager.clear();
    processor.process(mail.id());
    assertThat(jobs.find(mail.id()).orElseThrow().resultCode()).isEqualTo("TRIAL_CHANGED");
    jdbc.update("UPDATE user_profile SET subscription_status = 'ACTIVE' WHERE id = ?", USER_ID);
    entityManager.clear();
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(false, false));
    NotificationJob pushJob = job("TRIAL_PUSH");
    processor.process(pushJob.id());
    assertThat(jobs.find(pushJob.id()).orElseThrow().resultCode()).isEqualTo("CHANNEL_DISABLED");
    verifyNoInteractions(sender, push);
  }

  @DisplayName("재시도 가능한 거절만 재시도하고 결과가 불명확한 전송은 자동 재발송하지 않는다.")
  @Test
  void retryableRejectionRetriesButUnknownResponseNeverAutomaticallyResends() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    NotificationJob mail = job("TRIAL_EMAIL");
    when(sender.send(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(
            new EmailSendResult(Status.RETRYABLE, null), new EmailSendResult(Status.UNKNOWN, null));
    assertThatThrownBy(() -> processor.process(mail.id()))
        .isInstanceOf(RetryablePushNotificationException.class);
    assertThat(jobs.find(mail.id()).orElseThrow().status()).isEqualTo("PENDING");
    processor.process(mail.id());
    processor.process(mail.id());
    assertThat(jobs.find(mail.id()).orElseThrow().status()).isEqualTo("UNKNOWN");
    verify(sender, times(2)).send(anyString(), anyString(), anyString(), anyString());
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().status()).isEqualTo("PENDING");
  }

  @DisplayName("선점 후 중단된 이메일은 재발송하지 않고 기한 지난 작업은 생략한다.")
  @Test
  void crashAfterClaimDoesNotResendEmailAndLateJobsAreSkipped() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    NotificationJob mail = job("TRIAL_EMAIL");
    assertThat(jobs.claim(mail.id())).isPresent();
    when(clock.instant()).thenReturn(NOW.plusSeconds(301));
    processor.process(mail.id());
    assertThat(jobs.find(mail.id()).orElseThrow().status()).isEqualTo("UNKNOWN");
    when(clock.instant()).thenReturn(NOW.plusSeconds(7201));
    NotificationJob pushJob = job("TRIAL_PUSH");
    processor.process(pushJob.id());
    assertThat(jobs.find(pushJob.id()).orElseThrow().resultCode()).isEqualTo("TOO_LATE");
    verifyNoInteractions(sender, push);
  }

  @DisplayName("샌드박스 이벤트나 허용 목록에 없는 상품은 알림 작업을 만들지 않는다.")
  @Test
  void sandboxAndUnlistedProductsDoNotCreateJobs() {
    jobs.recordTrial(USER_ID, "SANDBOX");
    assertThat(jobs.pendingReservations()).isEmpty();
    jdbc.update(
        "UPDATE user_profile SET subscription_product_id = 'monthly' WHERE id = ?", USER_ID);
    entityManager.clear();
    jobs.recordTrial(USER_ID, "PRODUCTION");
    assertThat(jobs.pendingReservations()).isEmpty();
  }

  @DisplayName("프로모션 권한 부여는 체험 종료 알림을 예약하지 않는다.")
  @Test
  void promotionalGrantDoesNotScheduleTrialReminders() throws Exception {
    grantPromotionalSubscription();
    assertThat(jobs.pendingReservations()).isEmpty();
  }

  @DisplayName("프로모션 권한으로 바뀌면 이미 예약된 체험 종료 알림도 제외한다.")
  @Test
  void promotionalGrantExcludesPreviouslyScheduledTrialReminders() throws Exception {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    grantPromotionalSubscription();
    processor.process(job("TRIAL_PUSH").id());
    processor.process(job("TRIAL_EMAIL").id());
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().resultCode())
        .isEqualTo("TRIAL_CHANGED");
    assertThat(jobs.find(job("TRIAL_EMAIL").id()).orElseThrow().resultCode())
        .isEqualTo("TRIAL_CHANGED");
    verifyNoInteractions(sender, push);
  }

  private void grantPromotionalSubscription() throws Exception {
    String body =
        """
        {"api_version":"1.0","event":{"id":"%s","type":"NON_RENEWING_PURCHASE",
          "app_user_id":"%s","environment":"PRODUCTION","period_type":"PROMOTIONAL",
          "product_id":"rc_promo_premium_monthly","store":"PROMOTIONAL",
          "event_timestamp_ms":%d,"expiration_at_ms":%d,"purchased_at_ms":%d}}
        """
            .formatted(
                UUID.randomUUID(),
                USER_ID,
                NOW.toEpochMilli(),
                NOW.plusSeconds(30 * 86400).toEpochMilli(),
                NOW.toEpochMilli());
    mvc.perform(
            post("/webhooks/revenuecat")
                .header("Authorization", "test-reminder-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/me/subscription").with(user(new AuthUserPrincipal(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.premium").value(true))
        .andExpect(jsonPath("$.data.isTrial").value(false))
        .andExpect(jsonPath("$.data.periodType").value("PROMOTIONAL"));
  }

  @DisplayName("관리자는 임의 수신자에게 테스트 발송하며 멱등성과 입력값을 검증한다.")
  @Test
  void adminCanTestArbitraryRecipientWithIdempotencyAndValidation() throws Exception {
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(false, false));
    String key = UUID.randomUUID().toString();
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/api/v1/admin/notifications/email-tests")
                  .with(user(new AuthUserPrincipal(USER_ID)))
                  .header("Idempotency-Key", key)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"recipient\":\"arbitrary@example.org\"}"))
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
    assertThat(jobs.pendingReservations()).hasSize(1);
    mvc.perform(
            post("/api/v1/admin/notifications/email-tests")
                .with(user(new AuthUserPrincipal(USER_ID)))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipient\":\"different@example.org\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/admin/notifications/email-tests")
                .with(user(new AuthUserPrincipal(USER_ID)))
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipient\":\"invalid\"}"))
        .andExpect(status().isBadRequest());
    when(sender.send(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new EmailSendResult(Status.ACCEPTED, "test-mail"));
    processor.process(job("TEST_EMAIL").id());
    verify(sender).send(eq("arbitrary@example.org"), contains("테스트"), anyString(), anyString());
  }

  @DisplayName("관리자 설정 변경은 각 알림 예약 채널에 즉시 반영된다.")
  @Test
  void adminSettingsImmediatelyControlEachReservationChannel() throws Exception {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    mvc.perform(
            put("/api/v1/admin/notifications/trial-reminder-settings")
                .with(user(new AuthUserPrincipal(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pushEnabled\":false,\"emailEnabled\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.pushEnabled").value(false))
        .andExpect(jsonPath("$.data.emailEnabled").value(true));
    assertThat(jobs.pendingReservations())
        .extracting(NotificationJob::kind)
        .containsExactly("TRIAL_EMAIL");
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(true, false));
    assertThat(jobs.pendingReservations())
        .extracting(NotificationJob::kind)
        .containsExactly("TRIAL_PUSH");
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(false, false));
    assertThat(jobs.pendingReservations()).isEmpty();
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(true, true));
    assertThat(jobs.pendingReservations()).hasSize(2);
  }

  @DisplayName("예약 후 이메일을 꺼도 푸시 채널은 비활성화하지 않는다.")
  @Test
  void disablingEmailAfterReservationDoesNotDisablePush() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    NotificationJob mail = job("TRIAL_EMAIL");
    jobs.registered(mail.id());
    jdbc.update("UPDATE user_profile SET push_permission_status = 'GRANTED' WHERE id = ?", USER_ID);
    entityManager.clear();
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(true, false));
    processor.process(mail.id());
    processor.process(job("TRIAL_PUSH").id());
    assertThat(jobs.find(mail.id()).orElseThrow().resultCode()).isEqualTo("CHANNEL_DISABLED");
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().status()).isEqualTo("PROCESSED");
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(true, true));
    processor.process(mail.id());
    verifyNoInteractions(sender);
    verify(push).sendAll(org.mockito.ArgumentMatchers.anyList());
  }

  @DisplayName("푸시를 꺼도 이메일 발송은 막지 않는다.")
  @Test
  void disabledPushDoesNotPreventEmailDelivery() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    adminJobs.updateSettings(USER_ID, new TrialReminderSettings(false, true));
    when(sender.send(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new EmailSendResult(Status.ACCEPTED, "email-only"));
    processor.process(job("TRIAL_PUSH").id());
    processor.process(job("TRIAL_EMAIL").id());
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().resultCode())
        .isEqualTo("CHANNEL_DISABLED");
    assertThat(jobs.find(job("TRIAL_EMAIL").id()).orElseThrow().status()).isEqualTo("ACCEPTED");
    verifyNoInteractions(push);
    verify(sender).send(eq("member@example.com"), anyString(), anyString(), anyString());
  }

  @DisplayName("이메일 관리 API에는 관리자 권한과 인증이 필요하다.")
  @Test
  void emailApiRequiresAdminAndAuthentication() throws Exception {
    mvc.perform(post("/api/v1/admin/notifications/email-tests"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/admin/notifications/email-tests")
                .with(user(new AuthUserPrincipal(Long.MAX_VALUE))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/notifications/email-tests'].post").exists());
  }

  @DisplayName("웹훅의 구독 갱신과 알림 예약을 같은 트랜잭션에서 처리한다.")
  @Test
  void webhookKeepsSubscriptionAndReservationsInTheSameTransaction() throws Exception {
    long expiry = NOW.plusSeconds(7 * 86400).toEpochMilli();
    String body =
        """
        {"api_version":"1.0","event":{"id":"trial-webhook-test","type":"INITIAL_PURCHASE",
          "app_user_id":"%s","environment":"PRODUCTION","period_type":"TRIAL",
          "product_id":"annual","store":"APP_STORE","event_timestamp_ms":%d,
          "expiration_at_ms":%d,"purchased_at_ms":%d}}
        """
            .formatted(USER_ID, NOW.toEpochMilli(), expiry, NOW.toEpochMilli());
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/webhooks/revenuecat")
                  .header("Authorization", "test-reminder-secret")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk());
    }
    assertThat(jobs.pendingReservations()).hasSize(2);
    assertThat(job("TRIAL_EMAIL").scheduledAt()).isEqualTo(NOW.plusSeconds(6 * 86400));
    TestTransaction.flagForRollback();
    TestTransaction.end();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_job WHERE user_profile_id = ?",
                Long.class,
                USER_ID))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM subscription_event WHERE event_id = ?",
                Long.class,
                "trial-webhook-test"))
        .isZero();
  }

  private NotificationJob job(String kind) {
    UUID id =
        jdbc.queryForObject(
            "SELECT id FROM notification_job WHERE user_profile_id = ? AND kind = ?",
            UUID.class,
            USER_ID,
            kind);
    return jobs.find(id).orElseThrow();
  }
}
