// 관리자 캠페인의 대상 고정, 배치 발송과 중복 방지를 실제 DB로 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushMessage;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Expo와 SQS만 모킹해 관리자 일괄 발송의 DB 경계를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:adminpushcore;MODE=PostgreSQL;"
            + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
class AdminPushCampaignIntegrationTests {

  private static final long ADMIN = 995500L;
  private static final long USER = 995501L;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private AdminPushRepository repository;
  @Autowired private AdminPushInputService input;
  @Autowired private AdminAuditService audit;
  @Autowired private PushDeliveryService deliveries;
  @Autowired private UserPushTokenDeliveryService tokens;

  private NotificationSender sender;
  private PushQueuePublisher queue;
  private AdminPushCampaignService campaigns;
  private AdminPushProcessingService processor;

  @BeforeEach
  void setup() {
    jdbc.update("delete from push_delivery");
    jdbc.update("delete from admin_push_target");
    jdbc.update("delete from admin_push_campaign");
    jdbc.update("delete from user_push_token");
    profile(ADMIN);
    profile(USER);
    sender = mock(NotificationSender.class);
    queue = mock(PushQueuePublisher.class);
    doAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              return null;
            })
        .when(queue)
        .publishAdminCampaign(org.mockito.ArgumentMatchers.any(UUID.class));
    when(sender.send(anyList()))
        .thenAnswer(
            invocation ->
                invocation.<List<PushMessage>>getArgument(0).stream()
                    .map(message -> PushTicketResult.accepted(UUID.randomUUID().toString()))
                    .toList());
    NotificationDispatchService dispatch =
        new NotificationDispatchService(
            tokens, deliveries, sender, queue, new SimpleMeterRegistry());
    campaigns = new AdminPushCampaignService(repository, input, audit, queue);
    processor = new AdminPushProcessingService(repository, deliveries, dispatch, queue);
  }

  @Test
  void sendsFrozenAudienceInBatchesAndIgnoresDuplicateWork() {
    for (int index = 0; index < 101; index++) {
      token(USER, "batch-" + index);
    }
    UUID id = create("batch");

    campaigns.send(id, ADMIN, "send");
    processor.process(id);
    processor.process(id);
    processor.process(id);

    verify(sender).send(org.mockito.ArgumentMatchers.argThat(messages -> messages.size() == 100));
    verify(sender).send(org.mockito.ArgumentMatchers.argThat(messages -> messages.size() == 1));
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
    assertThat(campaigns.detail(id).targetTokenCount()).isEqualTo(101);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from push_delivery where deduplication_key like ?",
                Long.class,
                "push:admin-broadcast:" + id + ":%"))
        .isEqualTo(101);
  }

  @Test
  void excludesDeactivatedSnapshotTargetsAndDoesNotIncludeLateTokens() {
    long previouslyRevoked = token(USER, "previously-revoked");
    jdbc.update("update user_push_token set status='REVOKED' where id=?", previouslyRevoked);
    final long active = token(USER, "active");
    final long excluded = token(USER, "excluded");
    UUID id = create("frozen");
    campaigns.send(id, ADMIN, "send");
    assertThat(campaigns.detail(id).excludedCount()).isZero();

    jdbc.update("update user_push_token set status='ACTIVE' where id=?", previouslyRevoked);
    jdbc.update("update user_push_token set status='REVOKED' where id=?", excluded);
    token(USER, "late");
    processor.process(id);

    assertThat(campaigns.detail(id).targetTokenCount()).isEqualTo(2);
    assertThat(campaigns.detail(id).excludedCount()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select user_push_token_id from push_delivery", Long.class))
        .isEqualTo(active);
  }

  @Test
  void excludesTokenWhoseOwnerChangedAfterAudienceCapture() {
    long moved = token(USER, "moved");
    UUID id = create("moved");
    campaigns.send(id, ADMIN, "send");

    jdbc.update("update user_push_token set user_profile_id=? where id=?", ADMIN, moved);
    processor.process(id);

    assertThat(campaigns.detail(id).excludedCount()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from push_delivery", Long.class)).isZero();
  }

  @Test
  void retriesReceiptSchedulingWithoutSendingExpoAgain() {
    token(USER, "receipt-retry-1");
    token(USER, "receipt-retry-2");
    UUID id = create("receipt-retry");
    campaigns.send(id, ADMIN, "send");
    doThrow(new IllegalStateException("SQS failure"))
        .doNothing()
        .when(queue)
        .scheduleReceiptCheck(anyLong(), eq(1));

    assertThatThrownBy(() -> processor.process(id)).isInstanceOf(IllegalStateException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from push_delivery where status='TICKET_ACCEPTED'", Long.class))
        .isEqualTo(2);
    processor.process(id);

    verify(sender, times(1)).send(anyList());
    verify(queue, times(3)).scheduleReceiptCheck(anyLong(), eq(1));
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
  }

  @Test
  void waitsForAnotherConsumerBeforeAdvancingThePage() {
    long tokenId = token(USER, "concurrent");
    UUID id = create("concurrent");
    campaigns.send(id, ADMIN, "send");
    PreparedPushDelivery claimed =
        deliveries
            .prepare(
                new PreparePushDeliveryCommand(
                    USER,
                    tokenId,
                    NotificationType.ADMIN_BROADCAST,
                    "push:admin-broadcast:" + id + ":" + USER + ":" + tokenId,
                    "공지",
                    "내용",
                    "/home"))
            .orElseThrow();

    assertThatThrownBy(() -> processor.process(id))
        .isInstanceOf(RetryablePushNotificationException.class);
    assertThat(repository.find(id).getFirst().lastTargetId()).isZero();

    deliveries.recordTicketResult(claimed.pushDeliveryId(), PushTicketResult.accepted("ticket"));
    processor.process(id);
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
  }

  @Test
  void testAndBroadcastUseSeparateKeysAndLeaveLearningStateUntouched() {
    token(ADMIN, "admin");
    UUID id = create("separate");

    campaigns.test(id, ADMIN, "test");
    campaigns.test(id, ADMIN, "test");
    processor.test(id, ADMIN, "test");
    processor.test(id, ADMIN, "test");
    campaigns.send(id, ADMIN, "send");
    processor.process(id);

    verify(sender, times(2)).send(anyList());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from push_delivery where user_profile_id=?", Long.class, ADMIN))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_notification_state where user_profile_id=?",
                Long.class,
                ADMIN))
        .isZero();
  }

  private UUID create(String key) {
    return campaigns.create(ADMIN, key, new AdminPushCampaignRequest("공지", "내용", "/home")).id();
  }

  private long token(long userId, String suffix) {
    String token = "ExponentPushToken[lan462-" + suffix + "]";
    jdbc.update(
        """
        insert into user_push_token (
          user_profile_id,platform,expo_push_token,status,created_at,updated_at
        ) values (?,'IOS',?,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
        """,
        userId,
        token);
    return jdbc.queryForObject(
        "select id from user_push_token where expo_push_token=?", Long.class, token);
  }

  private void profile(long id) {
    if (jdbc.queryForObject("select count(*) from user_profile where id=?", Long.class, id) == 0) {
      jdbc.update(
          """
          insert into user_profile (
            id,nickname,target_locale,base_locale,current_level,push_permission_status,
            status,created_at,updated_at
          ) values (?,'broadcast','EN','KR',1,'NOT_DETERMINED','ACTIVE',
            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
          """,
          id);
    }
    jdbc.update("update user_profile set status='ACTIVE' where id=?", id);
  }
}
