// 체험 예약과 이메일 테스트의 권한, 멱등성 및 채널별 발송 결과를 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.client.EmailSendResult;
import com.landit.landitbe.feature.notification.client.EmailSendResult.Status;
import com.landit.landitbe.feature.notification.client.EmailSender;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.dto.NotificationJob;
import com.landit.landitbe.feature.notification.dto.TrialReminderSettings;
import com.landit.landitbe.feature.notification.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.service.NotificationJobProcessingService;
import com.landit.landitbe.feature.notification.service.NotificationJobService;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
  @Autowired private UserProfileService profiles;
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
    ReflectionTestUtils.setField(jobs, "consumerEnabled", true);
    sender = mock(EmailSender.class);
    push = mock(NotificationDispatchService.class);
    processor =
        new NotificationJobProcessingService(
            jobs, profiles, push, sender, policy, clock, validator);
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

  @Test
  void trialChannelsStartEnabledAndUseEnabledColumnDefaults() {
    assertThat(jobs.settings()).isEqualTo(new TrialReminderSettings(true, true));
    jdbc.update("DELETE FROM trial_reminder_settings WHERE id = 1");
    jdbc.update("INSERT INTO trial_reminder_settings (id) VALUES (1)");
    assertThat(jobs.settings()).isEqualTo(new TrialReminderSettings(true, true));
  }

  @Test
  void deniedPushStillSendsEmailOnlyOnce() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    jobs.recordTrial(USER_ID, "PRODUCTION");
    assertThat(jobs.pendingReservations()).hasSize(2);
    when(sender.send(anyString(), anyString(), anyString()))
        .thenReturn(new EmailSendResult(Status.ACCEPTED, "ses-1"));
    NotificationJob mail = job("TRIAL_EMAIL");
    processor.process(job("TRIAL_PUSH").id());
    processor.process(mail.id());
    processor.process(mail.id());
    assertThat(jobs.find(mail.id()).orElseThrow().providerMessageId()).isEqualTo("ses-1");
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().resultCode())
        .isEqualTo("PUSH_PERMISSION_DENIED");
    verify(sender, times(1)).send(eq("member@example.com"), anyString(), contains("발신 전용"));
    verifyNoInteractions(push);
  }

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
    jobs.updateSettings(USER_ID, new TrialReminderSettings(false, false));
    NotificationJob pushJob = job("TRIAL_PUSH");
    processor.process(pushJob.id());
    assertThat(jobs.find(pushJob.id()).orElseThrow().resultCode()).isEqualTo("CHANNEL_DISABLED");
    verifyNoInteractions(sender, push);
  }

  @Test
  void retryableRejectionRetriesButUnknownResponseNeverAutomaticallyResends() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    NotificationJob mail = job("TRIAL_EMAIL");
    when(sender.send(anyString(), anyString(), anyString()))
        .thenReturn(
            new EmailSendResult(Status.RETRYABLE, null), new EmailSendResult(Status.UNKNOWN, null));
    assertThatThrownBy(() -> processor.process(mail.id()))
        .isInstanceOf(RetryablePushNotificationException.class);
    assertThat(jobs.find(mail.id()).orElseThrow().status()).isEqualTo("PENDING");
    processor.process(mail.id());
    processor.process(mail.id());
    assertThat(jobs.find(mail.id()).orElseThrow().status()).isEqualTo("UNKNOWN");
    verify(sender, times(2)).send(anyString(), anyString(), anyString());
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().status()).isEqualTo("PENDING");
  }

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

  @Test
  void adminCanTestArbitraryRecipientWithIdempotencyAndValidation() throws Exception {
    jobs.updateSettings(USER_ID, new TrialReminderSettings(false, false));
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
    when(sender.send(anyString(), anyString(), anyString()))
        .thenReturn(new EmailSendResult(Status.ACCEPTED, "test-mail"));
    processor.process(job("TEST_EMAIL").id());
    verify(sender).send(eq("arbitrary@example.org"), contains("테스트"), anyString());
  }

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
    jobs.updateSettings(USER_ID, new TrialReminderSettings(true, false));
    assertThat(jobs.pendingReservations())
        .extracting(NotificationJob::kind)
        .containsExactly("TRIAL_PUSH");
    jobs.updateSettings(USER_ID, new TrialReminderSettings(false, false));
    assertThat(jobs.pendingReservations()).isEmpty();
    jobs.updateSettings(USER_ID, new TrialReminderSettings(true, true));
    assertThat(jobs.pendingReservations()).hasSize(2);
  }

  @Test
  void disablingEmailAfterReservationDoesNotDisablePush() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    NotificationJob mail = job("TRIAL_EMAIL");
    jobs.registered(mail.id());
    jdbc.update("UPDATE user_profile SET push_permission_status = 'GRANTED' WHERE id = ?", USER_ID);
    entityManager.clear();
    jobs.updateSettings(USER_ID, new TrialReminderSettings(true, false));
    processor.process(mail.id());
    processor.process(job("TRIAL_PUSH").id());
    assertThat(jobs.find(mail.id()).orElseThrow().resultCode()).isEqualTo("CHANNEL_DISABLED");
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().status()).isEqualTo("PROCESSED");
    jobs.updateSettings(USER_ID, new TrialReminderSettings(true, true));
    processor.process(mail.id());
    verifyNoInteractions(sender);
    verify(push).sendAll(org.mockito.ArgumentMatchers.anyList());
  }

  @Test
  void disabledPushDoesNotPreventEmailDelivery() {
    jobs.recordTrial(USER_ID, "PRODUCTION");
    jobs.updateSettings(USER_ID, new TrialReminderSettings(false, true));
    when(sender.send(anyString(), anyString(), anyString()))
        .thenReturn(new EmailSendResult(Status.ACCEPTED, "email-only"));
    processor.process(job("TRIAL_PUSH").id());
    processor.process(job("TRIAL_EMAIL").id());
    assertThat(jobs.find(job("TRIAL_PUSH").id()).orElseThrow().resultCode())
        .isEqualTo("CHANNEL_DISABLED");
    assertThat(jobs.find(job("TRIAL_EMAIL").id()).orElseThrow().status()).isEqualTo("ACCEPTED");
    verifyNoInteractions(push);
    verify(sender).send(eq("member@example.com"), anyString(), anyString());
  }

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

  @Test
  void webhookCommitsSubscriptionAndTwoReservationsAtomically() throws Exception {
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
