// 관리자 캠페인의 대상 고정, 배치 발송과 중복 방지를 실제 DB로 검증한다.

package com.landit.landitbe.feature.notification.campaign.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.notification.NotificationProperties;
import com.landit.landitbe.feature.audit.service.AdminAuditService;
import com.landit.landitbe.feature.notification.campaign.admin.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.campaign.domain.AdminPushAudienceType;
import com.landit.landitbe.feature.notification.campaign.repository.AdminPushRepository;
import com.landit.landitbe.feature.notification.campaign.service.AdminPushProcessingService;
import com.landit.landitbe.feature.notification.delivery.client.NotificationSender;
import com.landit.landitbe.feature.notification.delivery.client.PushMessage;
import com.landit.landitbe.feature.notification.delivery.client.PushNotificationException;
import com.landit.landitbe.feature.notification.delivery.client.PushTicketResult;
import com.landit.landitbe.feature.notification.delivery.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.delivery.dto.PreparePushDeliveryCommand;
import com.landit.landitbe.feature.notification.delivery.dto.PreparedPushDelivery;
import com.landit.landitbe.feature.notification.delivery.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.delivery.messaging.SqsPushQueuePublisher;
import com.landit.landitbe.feature.notification.delivery.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.delivery.service.PushDeliveryService;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.token.service.UserPushTokenDeliveryService;
import com.landit.landitbe.shared.exception.ApiException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import tools.jackson.databind.json.JsonMapper;

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
  @MockitoSpyBean private PushDeliveryService deliveries;
  @Autowired private UserPushTokenDeliveryService tokens;

  private NotificationSender sender;
  private PushQueuePublisher queue;
  private AdminPushCampaignService campaigns;
  private AdminPushProcessingService processor;
  private AdminPushAudienceSqlService sql;
  private com.landit.landitbe.feature.notification.campaign.messaging.AdminPushScheduler scheduler;

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
    scheduler =
        mock(com.landit.landitbe.feature.notification.campaign.messaging.AdminPushScheduler.class);
    campaigns = new AdminPushCampaignService(repository, input, audit, queue, sql, scheduler);
    processor = new AdminPushProcessingService(repository, deliveries, dispatch, queue, campaigns);
  }

  @DisplayName("캠페인 50개를 쿼리 4회로 조회하면서 상세 정보를 보존한다.")
  @Test
  void listsFiftyCampaignsWithFourQueriesAndPreservesDetails() throws Exception {
    token(USER, "batch-list");
    UUID sent = campaigns.create(ADMIN, "batch-sent", selected(List.of(USER))).id();
    campaigns.test(sent, ADMIN, "test");
    campaigns.send(sent, ADMIN, "send");
    processor.process(sent);
    jdbc.update(
        "update push_delivery set status='DELIVERED' where notification_type='ADMIN_BROADCAST'");
    for (int index = 0; index < 49; index++) {
      campaigns.create(
          ADMIN,
          "batch-draft-" + index,
          new AdminPushCampaignRequest(
              "공지",
              "내용",
              "/home",
              AdminPushAudienceType.SELECTED,
              List.of(USER),
              null,
              List.of(ADMIN)));
    }
    var source = org.mockito.Mockito.spy(jdbc.getDataSource());
    var listing =
        new AdminPushCampaignService(
            new AdminPushRepository(new JdbcTemplate(source)), input, audit, queue, sql, scheduler);
    var page = listing.list(null, null, 0, 50);
    assertThat(page.items()).hasSize(50);
    var sentItem =
        page.items().stream().filter(item -> item.id().equals(sent)).findFirst().orElseThrow();
    assertThat(sentItem.succeededCount()).isEqualTo(1);
    assertThat(sentItem.pendingCount()).isZero();
    assertThat(sentItem.failedCount()).isZero();
    assertThat(sentItem.excludedCount()).isZero();
    verify(source, times(4)).getConnection();
    for (var item : page.items()) {
      assertThat(item).isEqualTo(campaigns.detail(item.id()));
    }
    org.mockito.Mockito.clearInvocations(source);
    assertThat(listing.list(null, null, 0, 1).items()).hasSize(1);
    verify(source, times(4)).getConnection();
  }

  @DisplayName("선정한 사용자가 모두 제외되면 캠페인 대상 수 0을 유지한다.")
  @Test
  void preservesZeroAudienceWhenEverySelectedUserIsExcluded() {
    var request =
        new AdminPushCampaignRequest(
            "공지",
            "내용",
            "/home",
            AdminPushAudienceType.SELECTED,
            List.of(USER),
            null,
            List.of(USER));
    UUID id = campaigns.create(ADMIN, "all-excluded", request).id();
    assertThat(campaigns.create(ADMIN, "all-excluded", request).id()).isEqualTo(id);
    assertThat(campaigns.preview(id).estimatedTokenCount()).isZero();
    campaigns.send(id, ADMIN, "send");
    processor.process(id);
    assertThat(campaigns.detail(id).targetTokenCount()).isZero();
  }

  @DisplayName("캠페인 하나에 사용자 1,000명 넘게 지정할 수 있다.")
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

  @DisplayName("캠페인을 예약과 상태로 필터링하며 페이지 전체 건수를 반환한다.")
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

  @DisplayName("캠페인이 없으면 빈 목록을 반환하고 잘못된 필터는 거부한다.")
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

  @DisplayName("발송 시 SQL 대상을 조회하고 수동 선택과 제외 대상을 함께 적용한다.")
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

  @DisplayName("예약 등록을 재시도하며 예약 시각 전 발송과 변경된 시각의 요청을 거부한다.")
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

  @DisplayName("큐 메시지가 도착해도 취소된 예약은 발송하지 않는다.")
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

  @DisplayName("예약 SQL은 최신 조회 결과를 사용하며 빈 결과를 전체 사용자로 확대하지 않는다.")
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

  @DisplayName("대상 확정 전 취소가 발생하면 SQL 조회 후에도 발송을 중단한다.")
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

  @DisplayName("선택한 활성 사용자에게만 발송하고 관리자 테스트 발송은 독립적으로 처리한다.")
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

  @DisplayName("동일한 선택 대상 생성은 중복 처리하지 않고 대상 변경이나 사용자 누락은 거부한다.")
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

  @DisplayName("선택 대상 저장이 실패하면 캠페인과 사용자 목록을 함께 롤백한다.")
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

  @DisplayName("확정한 대상을 묶음으로 발송하고 중복 작업은 무시한다.")
  @Test
  void sendsFrozenAudienceInBatchesAndIgnoresDuplicateWork() {
    for (int index = 0; index < 101; index++) {
      token(USER, "batch-" + index);
    }
    UUID id = create("batch");

    campaigns.send(id, ADMIN, "send");
    processor.process(id);
    verify(deliveries).prepareAll(argThat(commands -> commands.size() == 100));
    verify(deliveries)
        .recordTicketResults(
            argThat(ids -> ids.size() == 100), argThat(results -> results.size() == 100));
    verify(deliveries, never()).prepare(org.mockito.ArgumentMatchers.any());
    verify(deliveries, never()).recordTicketResult(anyLong(), org.mockito.ArgumentMatchers.any());
    verify(queue).scheduleReceiptChecks(argThat(ids -> ids.size() == 100), eq(1));
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
    verify(queue).scheduleReceiptChecks(argThat(ids -> ids.size() == 1), eq(1));
    verify(queue, never()).scheduleReceiptCheck(anyLong(), eq(1));
  }

  @DisplayName("성공과 실패 Ticket을 함께 커밋한 뒤 성공 이력만 Receipt 확인을 예약한다.")
  @Test
  void commitsMixedTicketsBeforeSchedulingOnlyAcceptedReceipts() {
    long first = token(USER, "mixed-first");
    final long second = token(USER, "mixed-second");
    UUID id = create("mixed");
    campaigns.send(id, ADMIN, "send");
    when(sender.send(anyList()))
        .thenAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              return List.of(
                  PushTicketResult.accepted("accepted"),
                  PushTicketResult.failed("DeviceNotRegistered"));
            });
    doAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              assertThat(
                      jdbc.queryForList(
                          "select status from push_delivery order by id", String.class))
                  .containsExactly("TICKET_ACCEPTED", "FAILED");
              List<Long> ids = invocation.getArgument(0);
              assertThat(ids).hasSize(1);
              assertThat(
                      jdbc.queryForObject(
                          "select user_push_token_id from push_delivery where id=?",
                          Long.class,
                          ids.getFirst()))
                  .isEqualTo(first);
              return null;
            })
        .when(queue)
        .scheduleReceiptChecks(anyList(), eq(1));

    processor.process(id);

    assertThat(
            jdbc.queryForObject(
                "select status from user_push_token where id=?", String.class, second))
        .isEqualTo("REVOKED");
    assertThat(campaigns.detail(id).failedCount()).isEqualTo(1);
    verify(queue).scheduleReceiptChecks(anyList(), eq(1));
  }

  @DisplayName("동시 캠페인 처리는 같은 페이지를 재전송하거나 처리 중 커서를 넘기지 않는다.")
  @Test
  void concurrentCampaignProcessingSendsEachTokenOnlyOnce() throws Exception {
    for (int index = 0; index < 3; index++) {
      token(USER, "overlap-" + index);
    }
    UUID id = create("overlap");
    campaigns.send(id, ADMIN, "send");
    CountDownLatch sending = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(sender.send(anyList()))
        .thenAnswer(
            invocation -> {
              sending.countDown();
              assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
              return invocation.<List<PushMessage>>getArgument(0).stream()
                  .map(message -> PushTicketResult.accepted(UUID.randomUUID().toString()))
                  .toList();
            });
    try (var executor = Executors.newSingleThreadExecutor()) {
      var first = executor.submit(() -> processor.process(id));
      try {
        assertThat(sending.await(10, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> processor.process(id))
            .isInstanceOf(RetryablePushNotificationException.class);
        assertThat(repository.find(id).getFirst().lastTargetId()).isZero();
      } finally {
        release.countDown();
      }
      first.get(10, TimeUnit.SECONDS);
    }
    processor.process(id);
    verify(sender).send(argThat(messages -> messages.size() == 3));
    assertThat(jdbc.queryForObject("select count(*) from push_delivery", Long.class)).isEqualTo(3);
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
  }

  @DisplayName("Ticket 저장 실패 시 선점 이력을 임의 재전송하거나 커서를 넘기지 않는다.")
  @Test
  void holdsClaimedPageWhenTicketPersistenceFails() {
    token(USER, "persist-failure");
    UUID id = create("persist-failure");
    campaigns.send(id, ADMIN, "send");
    doThrow(new IllegalStateException("ticket write failure"))
        .when(deliveries)
        .recordTicketResults(anyList(), anyList());

    assertThatThrownBy(() -> processor.process(id)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> processor.process(id))
        .isInstanceOf(RetryablePushNotificationException.class);

    verify(sender).send(anyList());
    assertThat(repository.find(id).getFirst().lastTargetId()).isZero();
    assertThat(jdbc.queryForObject("select status from push_delivery", String.class))
        .isEqualTo("REQUESTED");
    verify(queue, never()).scheduleReceiptChecks(anyList(), eq(1));
  }

  @DisplayName("SQS 일부 예약 실패 뒤에도 Ticket을 보존하고 10건 단위 예약만 재시도한다.")
  @Test
  void retriesPartialSqsBatchesWithoutResendingPush() {
    for (int index = 0; index < 12; index++) {
      token(USER, "sqs-partial-" + index);
    }
    UUID id = create("sqs-partial");
    campaigns.send(id, ADMIN, "send");
    var sqs = mock(SqsAsyncClient.class);
    var requests = new java.util.ArrayList<SendMessageBatchRequest>();
    when(sqs.sendMessageBatch(org.mockito.ArgumentMatchers.any(SendMessageBatchRequest.class)))
        .thenAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              SendMessageBatchRequest request = invocation.getArgument(0);
              requests.add(request);
              var result = SendMessageBatchResponse.builder();
              if (requests.size() == 2) {
                result.failed(
                    BatchResultErrorEntry.builder().id("0").code("InternalError").build());
              } else {
                result.successful(
                    request.entries().stream()
                        .map(entry -> SendMessageBatchResultEntry.builder().id(entry.id()).build())
                        .toList());
              }
              return CompletableFuture.completedFuture(result.build());
            });
    var publisher =
        new SqsPushQueuePublisher(
            sqs,
            JsonMapper.builder().build(),
            new NotificationProperties(
                "https://exp.host",
                null,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                "https://sqs.example.test/push",
                900));
    var dispatch =
        new NotificationDispatchService(
            tokens, deliveries, sender, publisher, new SimpleMeterRegistry());
    var processing =
        new AdminPushProcessingService(repository, deliveries, dispatch, queue, campaigns);

    assertThatThrownBy(() -> processing.process(id)).isInstanceOf(PushNotificationException.class);
    assertThat(repository.find(id).getFirst().lastTargetId()).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from push_delivery where status='TICKET_ACCEPTED'", Long.class))
        .isEqualTo(12);
    // 접수 이후 토큰이 폐기돼도 기존 Receipt 예약은 복구해야 한다.
    jdbc.update("update user_push_token set status='REVOKED'");
    processing.process(id);

    assertThat(requests)
        .extracting(request -> request.entries().size())
        .containsExactly(10, 2, 10, 2);
    assertThat(requests.stream().flatMap(request -> request.entries().stream()))
        .allSatisfy(entry -> assertThat(entry.delaySeconds()).isEqualTo(900));
    verify(sender).send(anyList());
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
  }

  @DisplayName("확정 후 비활성화된 대상은 제외하고 뒤늦게 등록된 토큰은 포함하지 않는다.")
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

  @DisplayName("대상 확정 후 소유자가 바뀐 토큰에는 발송하지 않는다.")
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

  @DisplayName("수신 확인 예약만 재시도하고 Expo 발송은 반복하지 않는다.")
  @Test
  void retriesReceiptSchedulingWithoutSendingExpoAgain() {
    token(USER, "receipt-retry-1");
    token(USER, "receipt-retry-2");
    UUID id = create("receipt-retry");
    campaigns.send(id, ADMIN, "send");
    doThrow(new IllegalStateException("SQS failure"))
        .doNothing()
        .when(queue)
        .scheduleReceiptChecks(anyList(), eq(1));

    assertThatThrownBy(() -> processor.process(id)).isInstanceOf(IllegalStateException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from push_delivery where status='TICKET_ACCEPTED'", Long.class))
        .isEqualTo(2);
    processor.process(id);

    verify(sender, times(1)).send(anyList());
    verify(queue, times(2)).scheduleReceiptChecks(argThat(ids -> ids.size() == 2), eq(1));
    verify(queue, never()).scheduleReceiptCheck(anyLong(), eq(1));
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
  }

  /** 정기 푸시와 달리 관리자 공지의 일시적 Expo 오류도 종료해 재발송하지 않는다. */
  @DisplayName("정기 푸시와 달리 관리자 공지의 일시적 Expo 오류도 종료해 재발송하지 않는다.")
  @Test
  void terminatesAdminRequestFailureWithoutResendingAfterBatchIntegration() {
    token(USER, "request-failure-1");
    token(USER, "request-failure-2");
    UUID id = create("request-failure");
    campaigns.send(id, ADMIN, "send");
    when(sender.send(anyList())).thenThrow(new RetryablePushNotificationException("Expo timeout"));

    processor.process(id);
    processor.process(id);

    verify(sender, times(1)).send(anyList());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from push_delivery "
                    + "where status='FAILED' and error_code='EXPO_REQUEST_UNCONFIRMED'",
                Long.class))
        .isEqualTo(2);
    assertThat(campaigns.detail(id).status()).isEqualTo("COMPLETED");
    assertThat(campaigns.detail(id).failedCount()).isEqualTo(2);
  }

  @DisplayName("다른 소비자의 처리가 끝나기 전에 다음 대상 페이지로 넘어가지 않는다.")
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

  @DisplayName("테스트와 전체 발송의 키를 구분하며 학습 상태를 변경하지 않는다.")
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
