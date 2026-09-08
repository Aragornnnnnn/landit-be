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
import com.landit.landitbe.feature.notification.domain.AdminPushAudienceType;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.repository.AdminPushRepository;
import com.landit.landitbe.shared.exception.ApiException;
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
  private AdminPushAudienceSqlService sql;
  private com.landit.landitbe.feature.notification.messaging.AdminPushScheduler scheduler;

  @BeforeEach
  void setup() {
    jdbc.update("delete from push_delivery");
    jdbc.update("delete from admin_push_target");
    jdbc.update("delete from admin_push_campaign_user");
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
    final NotificationDispatchService dispatch =
        new NotificationDispatchService(
            tokens, deliveries, sender, queue, new SimpleMeterRegistry());
    sql = mock(AdminPushAudienceSqlService.class);
    scheduler = mock(com.landit.landitbe.feature.notification.messaging.AdminPushScheduler.class);
    campaigns = new AdminPushCampaignService(repository, input, audit, queue, sql, scheduler);
    processor = new AdminPushProcessingService(repository, deliveries, dispatch, queue, campaigns);
  }

  @Test
  void supportsMoreThanOneThousandSelectedUsersInOneCampaign() {
    var ids = java.util.stream.LongStream.rangeClosed(996000, 997000).boxed().toList();
    ids.forEach(this::profile);
    token(ids.getFirst(), "first");
    token(ids.getLast(), "last");
    UUID id = campaigns.create(ADMIN, "large", selected(ids)).id();
    assertThat(campaigns.detail(id).userProfileIds()).hasSize(1001);
    assertThat(campaigns.preview(id).estimatedTokenCount()).isEqualTo(2);
    campaigns.send(id, ADMIN, "send");
    processor.process(id);
    assertThat(campaigns.detail(id).targetTokenCount()).isEqualTo(2);
  }

  @Test
  void filtersCampaignsByReservationAndStatusWithPageTotals() {
    List<String> statuses =
        List.of("SCHEDULE_PENDING", "SCHEDULED", "QUEUED", "SENDING", "COMPLETED", "CANCELLED");
    var ids = new java.util.ArrayList<UUID>();
    for (int index = 0; index < statuses.size(); index++) {
      UUID id = create("list-" + index);
      ids.add(id);
      jdbc.update(
          "update admin_push_campaign set status=?,scheduled_at=?,created_at=? where id=?",
          statuses.get(index),
          java.sql.Timestamp.from(
              java.time.Instant.parse("2026-09-10T10:00:00Z").minusSeconds(index)),
          java.time.LocalDateTime.of(2026, 9, 8, 10, 0).plusSeconds(index),
          id);
    }
    final UUID draft = create("not-scheduled");
    UUID pending = create("immediate-pending");
    jdbc.update("update admin_push_campaign set status='PENDING' where id=?", pending);
    UUID immediate = create("immediate-completed");
    jdbc.update("update admin_push_campaign set status='COMPLETED' where id=?", immediate);

    var all = campaigns.list(null, null, 0, 20);
    assertThat(all.totalCount()).isEqualTo(9);
    assertThat(all.items()).hasSize(9);
    var unreserved = campaigns.list(false, null, 0, 20);
    assertThat(unreserved.totalCount()).isEqualTo(3);
    assertThat(unreserved.items())
        .extracting(item -> item.id())
        .containsExactlyInAnyOrder(draft, pending, immediate);
    assertThat(campaigns.list(null, "COMPLETED", 0, 20).totalCount()).isEqualTo(2);
    assertThat(campaigns.list(false, "COMPLETED", 0, 20).items())
        .extracting(item -> item.id())
        .containsExactly(immediate);
    assertThat(campaigns.list(false, "SCHEDULED", 0, 20).totalCount()).isZero();
    assertThat(campaigns.list(null, "DRAFT", 0, 20).items())
        .extracting(item -> item.id())
        .containsExactly(draft);
    assertThat(campaigns.list(null, "PENDING", 0, 20).items())
        .extracting(item -> item.id())
        .containsExactly(pending);
    var first = campaigns.list(true, null, 0, 2);
    assertThat(first.items()).extracting(item -> item.id()).containsExactly(ids.get(5), ids.get(4));
    assertThat(first.totalCount()).isEqualTo(6);
    assertThat(first.totalPages()).isEqualTo(3);
    assertThat(first.hasNext()).isTrue();
    assertThat(campaigns.list(true, null, 2, 2).hasNext()).isFalse();
    var beyond = campaigns.list(true, null, 3, 2);
    assertThat(beyond.items()).isEmpty();
    assertThat(beyond.totalCount()).isEqualTo(6);
    assertThat(beyond.totalPages()).isEqualTo(3);
    for (int index = 0; index < statuses.size(); index++) {
      var filtered = campaigns.list(true, statuses.get(index), 0, 20);
      assertThat(filtered.items()).extracting(item -> item.id()).containsExactly(ids.get(index));
      assertThat(filtered.totalCount()).isEqualTo(1);
      assertThat(filtered.totalPages()).isEqualTo(1);
    }
  }

  @Test
  void handlesEmptyCampaignListAndRejectsInvalidFilters() {
    var page = campaigns.list(true, null, 0, 20);
    assertThat(page.items()).isEmpty();
    assertThat(page.totalCount()).isZero();
    assertThat(page.totalPages()).isZero();
    assertThat(page.hasNext()).isFalse();
    assertThatThrownBy(() -> campaigns.list(true, "UNKNOWN", 0, 20))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> campaigns.list(true, "", 0, 20)).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> campaigns.list(true, null, -1, 20)).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> campaigns.list(true, null, 0, 0)).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> campaigns.list(true, null, 0, 51)).isInstanceOf(ApiException.class);
  }

  @Test
  void resolvesSqlAtDispatchAndCombinesManualSelectionAndExclusions() {
    token(USER, "sql");
    token(ADMIN, "excluded");
    profile(USER + 1);
    token(USER + 1, "manual");
    String query = "select id as user_profile_id from user_profile";
    when(sql.query(query)).thenReturn(List.of(USER, ADMIN));
    UUID id =
        campaigns
            .create(
                ADMIN,
                "query",
                new AdminPushCampaignRequest(
                    "공지",
                    "내용",
                    "/home",
                    AdminPushAudienceType.SELECTED,
                    List.of(USER + 1, ADMIN),
                    query,
                    List.of(ADMIN)))
            .id();
    assertThat(campaigns.preview(id).estimatedTokenCount()).isEqualTo(2);
    campaigns.send(id, ADMIN, "send");
    assertThat(campaigns.detail(id).status()).isEqualTo("PENDING");
    // 예약/미리보기 후 응답한 사용자는 SQL 결과에서 사라진다.
    when(sql.query(query)).thenReturn(List.of(ADMIN));
    processor.process(id);
    processor.process(id);
    assertThat(campaigns.detail(id).targetTokenCount()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select user_profile_id from push_delivery", Long.class))
        .isEqualTo(USER + 1);
    verify(sql, times(2)).query(query);
  }

  @Test
  void retriesScheduleRegistrationAndRejectsEarlyDeliveryAndChangedTime() {
    token(USER, "scheduled");
    UUID id = create("schedule");
    var time =
        java.time.OffsetDateTime.now(java.time.ZoneOffset.ofHours(9)).plusHours(1).withNano(0);
    doThrow(new IllegalStateException("temporary scheduler failure"))
        .doNothing()
        .when(scheduler)
        .schedule(id, time.toInstant());
    assertThatThrownBy(() -> campaigns.schedule(id, ADMIN, "schedule", time))
        .isInstanceOf(IllegalStateException.class);
    assertThat(campaigns.detail(id).status()).isEqualTo("SCHEDULE_PENDING");
    campaigns.schedule(id, ADMIN, "schedule", time);
    campaigns.schedule(id, ADMIN, "schedule", time);
    verify(scheduler, times(2)).schedule(id, time.toInstant());
    assertThatThrownBy(() -> campaigns.schedule(id, ADMIN, "schedule", time.plusHours(1)))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> campaigns.send(id, ADMIN, "send")).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> processor.process(id))
        .isInstanceOf(RetryablePushNotificationException.class);
    assertThat(campaigns.detail(id).targetTokenCount()).isZero();
    jdbc.update(
        "update admin_push_campaign set scheduled_at=? where id=?",
        java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),
        id);
    processor.process(id);
    processor.process(id);
    assertThat(campaigns.detail(id).targetTokenCount()).isEqualTo(1);
    assertThatThrownBy(() -> campaigns.cancelSchedule(id, ADMIN)).isInstanceOf(ApiException.class);
  }

  @Test
  void ignoresCancelledScheduleEvenWhenQueuedMessageArrives() {
    token(USER, "cancelled");
    UUID id = create("cancel");
    var time =
        java.time.OffsetDateTime.now(java.time.ZoneOffset.ofHours(9)).plusHours(1).withNano(0);
    campaigns.schedule(id, ADMIN, "schedule", time);
    campaigns.cancelSchedule(id, ADMIN);
    campaigns.cancelSchedule(id, ADMIN);
    processor.process(id);
    assertThat(campaigns.detail(id).status()).isEqualTo("CANCELLED");
    org.mockito.Mockito.verifyNoInteractions(sender);
  }

  @Test
  void scheduledSqlUsesLatestResultsAndNeverFallsBackToAllWhenEmpty() {
    token(USER, "answered-later");
    String query = "select id as user_profile_id from user_profile";
    when(sql.query(query)).thenReturn(List.of(USER));
    UUID id =
        campaigns
            .create(
                ADMIN,
                "survey-schedule",
                new AdminPushCampaignRequest(
                    "공지",
                    "내용",
                    "/home",
                    AdminPushAudienceType.SELECTED,
                    List.of(),
                    query,
                    List.of()))
            .id();
    assertThat(campaigns.preview(id).estimatedUserCount()).isEqualTo(1);
    campaigns.schedule(
        id,
        ADMIN,
        "schedule",
        java.time.OffsetDateTime.now(java.time.ZoneOffset.ofHours(9)).plusHours(1).withNano(0));
    when(sql.query(query)).thenReturn(List.of());
    jdbc.update(
        "update admin_push_campaign set scheduled_at=? where id=?",
        java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),
        id);
    processor.process(id);
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
    assertThat(campaigns.detail(id).targetTokenCount()).isZero();
    org.mockito.Mockito.verifyNoInteractions(sender);
  }

  @Test
  void cancellationWinsAgainstSqlResolutionBeforeSnapshot() {
    token(USER, "cancel-race");
    String query = "select id as user_profile_id from user_profile";
    UUID id =
        campaigns
            .create(
                ADMIN,
                "race",
                new AdminPushCampaignRequest(
                    "공지",
                    "내용",
                    "/home",
                    AdminPushAudienceType.SELECTED,
                    List.of(),
                    query,
                    List.of()))
            .id();
    campaigns.schedule(
        id,
        ADMIN,
        "schedule",
        java.time.OffsetDateTime.now(java.time.ZoneOffset.ofHours(9)).plusHours(1).withNano(0));
    jdbc.update(
        "update admin_push_campaign set scheduled_at=? where id=?",
        java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),
        id);
    when(sql.query(query))
        .thenAnswer(
            invocation -> {
              campaigns.cancelSchedule(id, ADMIN);
              return List.of(USER);
            });
    processor.process(id);
    assertThat(campaigns.detail(id).targetTokenCount()).isZero();
    org.mockito.Mockito.verifyNoInteractions(sender);
  }

  @Test
  void sendsOnlySelectedActiveUsersAndKeepsAdminTestIndependent() {
    final long selectedToken = token(USER, "selected-1");
    final long revokedToken = token(USER, "selected-2");
    token(ADMIN, "not-selected-admin");
    profile(USER + 1);
    profile(USER + 2);
    token(USER + 1, "inactive");
    jdbc.update("update user_profile set status='BANNED' where id=?", USER + 1);
    UUID id =
        campaigns.create(ADMIN, "selected", selected(List.of(USER + 2, USER, USER + 1, USER))).id();

    assertThat(campaigns.detail(id).audienceType()).isEqualTo(AdminPushAudienceType.SELECTED);
    assertThat(campaigns.detail(id).userProfileIds()).containsExactly(USER, USER + 1, USER + 2);
    assertThat(campaigns.preview(id).estimatedUserCount()).isEqualTo(1);
    assertThat(campaigns.preview(id).estimatedTokenCount()).isEqualTo(2);
    campaigns.test(id, ADMIN, "test");
    processor.test(id, ADMIN, "test");
    campaigns.send(id, ADMIN, "send");
    campaigns.send(id, ADMIN, "send");
    jdbc.update("update user_push_token set status='REVOKED' where id=?", revokedToken);
    processor.process(id);
    processor.process(id);

    assertThat(campaigns.detail(id).targetTokenCount()).isEqualTo(2);
    assertThat(campaigns.detail(id).excludedCount()).isEqualTo(1);
    assertThat(
            jdbc.queryForList(
                "select user_push_token_id from push_delivery "
                    + "where notification_type='ADMIN_BROADCAST'",
                Long.class))
        .containsExactly(selectedToken);
    verify(sender, times(2)).send(anyList());
  }

  @Test
  void deduplicatesSelectedCreationAndRejectsChangedAudienceOrMissingUsers() {
    UUID id = campaigns.create(ADMIN, "selection-key", selected(List.of(USER, ADMIN))).id();
    assertThat(campaigns.create(ADMIN, "selection-key", selected(List.of(ADMIN, USER, USER))).id())
        .isEqualTo(id);
    assertThatThrownBy(() -> campaigns.create(ADMIN, "selection-key", selected(List.of(USER))))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                campaigns.create(
                    ADMIN, "selection-key", new AdminPushCampaignRequest("공지", "내용", "/home")))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> campaigns.create(ADMIN, "missing", selected(List.of(Long.MAX_VALUE))))
        .isInstanceOf(ApiException.class);
    assertThat(repository.byKey(ADMIN, "missing")).isEmpty();
    assertThat(campaigns.detail(id).userProfileIds()).containsExactly(ADMIN, USER);
  }

  private AdminPushCampaignRequest selected(List<Long> ids) {
    return new AdminPushCampaignRequest("공지", "내용", "/home", AdminPushAudienceType.SELECTED, ids);
  }

  @Test
  void rollsBackCampaignAndUsersWhenSelectionInsertFails() {
    UUID id = UUID.randomUUID();
    AdminPushRepository.Campaign campaign =
        new AdminPushRepository.Campaign(
            id,
            selected(List.of(USER, Long.MAX_VALUE)),
            ADMIN,
            "hash",
            "DRAFT",
            0,
            0,
            0,
            0,
            java.time.LocalDateTime.now(),
            null);
    assertThatThrownBy(() -> repository.insert(campaign, "rollback"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(repository.find(id)).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from admin_push_campaign_user where campaign_id=?",
                Long.class,
                id))
        .isZero();
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
