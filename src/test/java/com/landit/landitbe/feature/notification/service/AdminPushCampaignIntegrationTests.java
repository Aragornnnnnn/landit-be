// 관리자 캠페인의 대상 확정과 중복 제출 및 장애 복구를 실제 DB에서 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushMessage;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushRateLimitedException;
import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushRunView;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.shared.exception.ApiException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 외부 Expo를 모킹하고 DB의 실제 트랜잭션 경계를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:adminpushcore;MODE=PostgreSQL;"
            + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
class AdminPushCampaignIntegrationTests {
  private static final long ADMIN = 995500L;
  private static final long USER = 995501L;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AdminPushCampaignService campaigns;
  @Autowired private PushDeliveryService deliveries;
  @Autowired private UserPushTokenDeliveryService tokens;
  private NotificationSender sender;
  private NotificationDispatchService dispatch;
  private AdminPushProcessingService processor;

  @BeforeEach
  void setup() {
    jdbc.update("delete from push_delivery");
    jdbc.update("delete from admin_push_target");
    jdbc.update("delete from admin_push_run");
    jdbc.update("delete from admin_push_campaign");
    jdbc.update("delete from user_push_token where user_profile_id in (?,?)", ADMIN, USER);
    for (long id : List.of(ADMIN, USER)) {
      if (jdbc.queryForObject("select count(*) from user_profile where id=?", Long.class, id)
          == 0) {
        jdbc.update(
            """
            insert into user_profile ( id , nickname , target_locale , base_locale ,
            current_level , push_permission_status , status , created_at , updated_at )
            values ( ? , 'broadcast' , 'EN' , 'KR' , 1 , 'NOT_DETERMINED' , 'ACTIVE' ,
            CURRENT_TIMESTAMP , CURRENT_TIMESTAMP )
            """,
            id);
      }
    }
    jdbc.update("update user_profile set status='ACTIVE' where id in (?,?)", ADMIN, USER);
    releaseGate();
    sender = mock(NotificationSender.class);
    when(sender.send(anyList()))
        .thenAnswer(
            invocation ->
                ((List<PushMessage>) invocation.getArgument(0))
                    .stream()
                        .map(m -> PushTicketResult.accepted(UUID.randomUUID().toString()))
                        .toList());
    when(sender.getReceipt(anyString())).thenReturn(PushReceiptResult.delivered());
    when(sender.getReceipts(anyList()))
        .thenAnswer(
            invocation ->
                invocation.<List<String>>getArgument(0).stream().map(sender::getReceipt).toList());
    PushQueuePublisher publisher = mock(PushQueuePublisher.class);
    dispatch =
        new NotificationDispatchService(
            tokens, deliveries, sender, publisher, new SimpleMeterRegistry());
    processor =
        new AdminPushProcessingService(
            campaigns,
            dispatch,
            new PushReceiptService(deliveries, sender, publisher, new SimpleMeterRegistry()));
  }

  @Test
  void freezesAudienceAndExcludesChangedOwnershipWithoutAddingNewTokens() {
    token(ADMIN, "admin");
    final long moved = token(USER, "moved");
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    assertThat(campaigns.run(campaign, run.id()).targetTokenCount()).isEqualTo(2);
    token(USER, "late");
    jdbc.update("update user_push_token set user_profile_id=? where id=?", ADMIN, moved);
    tick(run);
    AdminPushRunView result = campaigns.run(campaign, run.id());
    assertThat(result.excludedCount()).isEqualTo(1);
    assertThat(result.ticketAcceptedCount()).isEqualTo(1);
    assertThat(result.targetTokenCount()).isEqualTo(2);
    verify(sender).send(org.mockito.ArgumentMatchers.argThat(list -> list.size() == 1));
  }

  @Test
  void concurrentCreationAndSendReturnOneIdentity() throws Exception {
    List<UUID> created = race(() -> campaigns.create(ADMIN, "same", content()).id());
    assertThat(created.get(0)).isEqualTo(created.get(1));
    UUID campaign = created.getFirst();
    List<UUID> sent =
        race(() -> campaigns.start(campaign, ADMIN, UUID.randomUUID().toString(), false).id());
    assertThat(sent.get(0)).isEqualTo(sent.get(1));
    assertThat(jdbc.queryForObject("select count(*) from admin_push_run", Long.class)).isEqualTo(1);
    assertThatThrownBy(
            () ->
                campaigns.create(
                    ADMIN, "same", new AdminPushCampaignRequest("other", "body", "/home")))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void separateTestAndCampaignsDoNotConsumeScheduledKeysOrLearningState() {
    final long token = token(ADMIN, "admin");
    UUID campaign = create();
    AdminPushRunView test = campaigns.start(campaign, ADMIN, "test", true);
    tick(test);
    tick(test);
    AdminPushRunView broadcast = campaigns.start(campaign, ADMIN, "send", false);
    tick(broadcast);
    tick(broadcast);
    UUID second = campaigns.create(ADMIN, "second", content()).id();
    AdminPushRunView next = campaigns.start(second, ADMIN, "send", false);
    tick(next);
    tick(next);
    dispatch.send(
        new SendPushNotificationCommand(
            "scheduled:2026-09-07:" + ADMIN,
            ADMIN,
            NotificationType.SMALL_TALK_REMINDER,
            "title",
            "body",
            "/home"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from push_delivery where user_push_token_id=?", Long.class, token))
        .isEqualTo(4);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_notification_state where user_profile_id=?",
                Long.class,
                ADMIN))
        .isZero();
    assertThat(campaigns.detail(campaign).broadcast().targetTokenCount()).isEqualTo(1);
  }

  @Test
  void uncertainSubmissionIsNotRetriedOnDuplicateMessages() {
    token(ADMIN, "admin");
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    when(sender.send(anyList())).thenThrow(new PushNotificationException("response lost"));
    long version = version(run);
    tick(run);
    processor.process(run.id(), version);
    AdminPushRunView result = campaigns.run(campaign, run.id());
    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.unknownCount()).isEqualTo(1);
    verify(sender, times(1)).send(anyList());
  }

  @Test
  void crashedClaimBecomesUnknownAndUnclaimedTargetsContinue() {
    token(ADMIN, "admin");
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    jdbc.update(
        """
        update admin_push_run set next_attempt_at=TIMESTAMP '2000-01-01 00:00:00'
        where id=?
        """,
        run.id());
    var work = campaigns.claim(run.id(), version(run)).orElseThrow();
    assertThat(campaigns.prepare(work).deliveries()).hasSize(1);
    jdbc.update(
        """
        update admin_push_run set lease_until=TIMESTAMP '2000-01-01 00:00:00' where
        id=?
        """,
        run.id());
    jdbc.update("update push_delivery set requested_at=TIMESTAMP '2000-01-01 00:00:00'");
    tick(run);
    assertThat(campaigns.run(campaign, run.id()).unknownCount()).isEqualTo(1);
    verifyNoInteractions(sender);
  }

  @Test
  void retriesOnlyExplicitRateLimitThreeTimesUsingSameDelivery() {
    token(ADMIN, "admin");
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    when(sender.send(anyList())).thenThrow(new PushRateLimitedException());
    for (int i = 0; i < 4; i++) {
      jdbc.update(
          """
          update push_delivery set admin_next_attempt_at=TIMESTAMP '2000-01-01 00:00:00'
          where admin_next_attempt_at is not null
          """);
      tick(run);
    }
    assertThat(campaigns.run(campaign, run.id()).failedCount()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from push_delivery", Long.class)).isEqualTo(1);
    verify(sender, times(4)).send(anyList());
  }

  @Test
  void receiptRecoveryRequiresNoNewSubmissionAndUnknownAfterThreeChecks() {
    token(ADMIN, "admin");
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    tick(run);
    when(sender.getReceipt(anyString())).thenReturn(PushReceiptResult.notReady());
    for (int i = 0; i < 3; i++) {
      jdbc.update(
          """
          update push_delivery set admin_receipt_next_at=TIMESTAMP '2000-01-01 00:00:00'
          """);
      tick(run);
    }
    assertThat(campaigns.run(campaign, run.id()).unknownCount()).isEqualTo(1);
    verify(sender, times(1)).send(anyList());
    verify(sender, times(3)).getReceipt(anyString());
  }

  @Test
  void receiptSuccessCompletesWithBalancedCounts() {
    token(ADMIN, "admin");
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    tick(run);
    jdbc.update(
        """
        update push_delivery set admin_receipt_next_at=TIMESTAMP '2000-01-01 00:00:00'
        """);
    tick(run);
    AdminPushRunView result = campaigns.run(campaign, run.id());
    assertThat(result.succeededCount()).isEqualTo(1);
    assertThat(result.pendingCount()).isZero();
    assertThat(result.status()).isEqualTo("COMPLETED");
  }

  @Test
  void emptyAudienceCompletesAndPublisherFailuresCanResumeSameRun() {
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    for (int i = 0; i < 8; i++) {
      jdbc.update(
          """
          update admin_push_run set next_attempt_at=TIMESTAMP '2000-01-01 00:00:00'
          where id=?
          """,
          run.id());
      campaigns.publications().forEach(campaigns::publicationFailed);
    }
    assertThat(campaigns.run(campaign, run.id()).status()).isEqualTo("BLOCKED");
    assertThat(campaigns.resume(campaign, run.id(), ADMIN).id()).isEqualTo(run.id());
    tick(run);
    assertThat(campaigns.run(campaign, run.id()).status()).isEqualTo("COMPLETED");
    verifyNoInteractions(sender);
  }

  @Test
  void chunksAt100AndNeverRequeriesNewTokens() {
    for (int i = 0; i < 101; i++) {
      token(USER, "token" + i);
    }
    UUID campaign = create();
    AdminPushRunView run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    tick(run);
    tick(run);
    verify(sender).send(org.mockito.ArgumentMatchers.argThat(list -> list.size() == 100));
    verify(sender).send(org.mockito.ArgumentMatchers.argThat(list -> list.size() == 1));
    assertThat(campaigns.run(campaign, run.id()).ticketAcceptedCount()).isEqualTo(101);
  }

  @Test
  void lateTicketReopensCompletedRunWithoutResubmitting() {
    token(ADMIN, "admin");
    UUID campaign = create();
    var run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    when(sender.send(anyList())).thenThrow(new PushNotificationException("response lost"));
    tick(run);
    assertThat(campaigns.run(campaign, run.id()).status()).isEqualTo("COMPLETED");
    long id = jdbc.queryForObject("select id from push_delivery", Long.class);
    deliveries.recordTicketResult(id, PushTicketResult.accepted("late-ticket"));
    campaigns.publications();
    assertThat(campaigns.run(campaign, run.id()).status()).isEqualTo("AWAITING_RECEIPTS");
    jdbc.update("update push_delivery set admin_receipt_next_at=TIMESTAMP '2000-01-01 00:00:00'");
    tick(run);
    assertThat(campaigns.run(campaign, run.id()).succeededCount()).isEqualTo(1);
    verify(sender, times(1)).send(anyList());
  }

  @Test
  void expiredReceiptCannotClearReplacementLease() {
    token(ADMIN, "admin");
    UUID campaign = create();
    var run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    tick(run);
    long id = jdbc.queryForObject("select id from push_delivery", Long.class);
    jdbc.update("update push_delivery set admin_receipt_next_at=TIMESTAMP '2000-01-01 00:00:00'");
    var first = deliveries.claimAdminReceipt(id).orElseThrow();
    jdbc.update(
        "update push_delivery set admin_receipt_lease_until=TIMESTAMP '2000-01-01 00:00:00'");
    var second = deliveries.claimAdminReceipt(id).orElseThrow();
    deliveries.deferAdminReceipt(id, first.attempt());
    assertThat(second.attempt()).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select admin_receipt_lease_until from push_delivery", java.sql.Timestamp.class))
        .isAfter(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
    deliveries.recordReceiptResult(id, PushReceiptResult.delivered());
    deliveries.deferAdminReceipt(id, second.attempt());
    assertThat(jdbc.queryForObject("select status from push_delivery", String.class))
        .isEqualTo("DELIVERED");
  }

  @Test
  void duplicateConsumersSubmitOneBatch() throws Exception {
    token(ADMIN, "admin");
    UUID campaign = create();
    var run = campaigns.start(campaign, ADMIN, "send", false);
    tick(run);
    jdbc.update("update admin_push_run set next_attempt_at=TIMESTAMP '2000-01-01 00:00:00'");
    releaseGate();
    long version = version(run);
    race(
        () -> {
          processor.process(run.id(), version);
          return true;
        });
    verify(sender, times(1)).send(anyList());
  }

  private UUID create() {
    return campaigns.create(ADMIN, UUID.randomUUID().toString(), content()).id();
  }

  private AdminPushCampaignRequest content() {
    return new AdminPushCampaignRequest("공지", "내용", "/home");
  }

  private long token(long user, String suffix) {
    String value = "ExponentPushToken[lan462-" + suffix + "]";
    jdbc.update(
        """
        insert into user_push_token ( user_profile_id , platform , expo_push_token ,
        status , created_at , updated_at ) values ( ? , 'IOS' , ? , 'ACTIVE' ,
        CURRENT_TIMESTAMP , CURRENT_TIMESTAMP )
        """,
        user,
        value);
    return jdbc.queryForObject(
        "select id from user_push_token where expo_push_token=?", Long.class, value);
  }

  private long version(AdminPushRunView run) {
    return jdbc.queryForObject(
        "select work_version from admin_push_run where id=?", Long.class, run.id());
  }

  private void tick(AdminPushRunView run) {
    jdbc.update(
        """
        update admin_push_run set next_attempt_at=TIMESTAMP '2000-01-01 00:00:00'
        where id=?
        """,
        run.id());
    releaseGate();
    processor.process(run.id(), version(run));
  }

  private void releaseGate() {
    jdbc.update(
        """
        update admin_push_gate set lease_owner=null , lease_until=null ,
        next_submission_at=TIMESTAMP '2000-01-01 00:00:00' where id=1
        """);
  }

  private <T> List<T> race(Callable<T> call) throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<T> task =
          () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return call.call();
          };
      var first = executor.submit(task);
      var second = executor.submit(task);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
    }
  }
}
