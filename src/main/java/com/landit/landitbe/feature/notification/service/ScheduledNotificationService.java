// 매일 예약된 학습 알림 대상을 페이지 단위로 계산하고 직접 발송한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.UserNotificationState;
import com.landit.landitbe.feature.notification.repository.UserNotificationStateRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** 매일 예약된 학습 알림 대상을 페이지 단위로 계산하고 직접 발송한다. */
@Slf4j
@Service
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class ScheduledNotificationService {

  private static final int PAGE_SIZE = 500;
  private static final String POLICY_VERSION = "v1";
  private static final ZoneId KOREA_TIME_ZONE = ZoneId.of("Asia/Seoul");

  private final NotificationTargetPageQueryService notificationTargetPageQueryService;
  private final NotificationTargetSelectionService notificationTargetSelectionService;
  private final UserNotificationStateRepository userNotificationStateRepository;
  private final NotificationDispatchService notificationDispatchService;
  private final TransactionOperations pageTransactions;
  private final MeterRegistry meterRegistry;

  /**
   * 페이지별 계산과 상태 저장을 독립 트랜잭션으로 실행하도록 서비스를 구성한다.
   *
   * @param notificationTargetPageQueryService 대상 계산용 일괄 조회 Service
   * @param notificationTargetSelectionService 사용자별 대상 선정 Service
   * @param userNotificationStateRepository 알림 상태 저장소
   * @param notificationDispatchService 페이지 단위 Push 발송 Service
   * @param transactionManager 애플리케이션 트랜잭션 관리자
   * @param meterRegistry 예약 알림 처리 지표 저장소
   */
  public ScheduledNotificationService(
      NotificationTargetPageQueryService notificationTargetPageQueryService,
      NotificationTargetSelectionService notificationTargetSelectionService,
      UserNotificationStateRepository userNotificationStateRepository,
      NotificationDispatchService notificationDispatchService,
      PlatformTransactionManager transactionManager,
      MeterRegistry meterRegistry) {
    this.notificationTargetPageQueryService = notificationTargetPageQueryService;
    this.notificationTargetSelectionService = notificationTargetSelectionService;
    this.userNotificationStateRepository = userNotificationStateRepository;
    this.notificationDispatchService = notificationDispatchService;
    this.meterRegistry = meterRegistry;
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.pageTransactions = transactionTemplate;
  }

  /**
   * Scheduler 기준 시각으로 모든 활성 사용자를 500명씩 계산해 페이지 단위로 직접 발송한다.
   *
   * @param messageId 동일한 예약 실행을 식별하는 Queue 메시지 ID
   * @param occurredAt EventBridge Scheduler가 지정한 예정 시각
   * @param visibilityExtender 현재 SQS 메시지의 visibility를 연장하는 작업
   */
  public void process(String messageId, Instant occurredAt, Runnable visibilityExtender) {
    String attemptId = UUID.randomUUID().toString();
    long batchStartedAt = System.nanoTime();
    long queueDelayMs = Math.max(0L, Duration.between(occurredAt, Instant.now()).toMillis());
    long lastUserProfileId = 0L;
    LocalDate scheduledDate = occurredAt.atZone(KOREA_TIME_ZONE).toLocalDate();
    BatchSummary summary = BatchSummary.empty();
    String failureStage = "start";
    meterRegistry
        .timer("landit.notification.scheduled.queue.delay")
        .record(queueDelayMs, TimeUnit.MILLISECONDS);
    log.info(
        "scheduled_notification_batch_started messageId={} attemptId={} scheduledDate={} "
            + "policyVersion={} queueDelayMs={}",
        messageId,
        attemptId,
        scheduledDate,
        POLICY_VERSION,
        queueDelayMs);
    try {
      while (true) {
        final long pageStartedAt = System.nanoTime();
        failureStage = "page_load";
        visibilityExtender.run();
        NotificationTargetPage page =
            notificationTargetPageQueryService.loadPage(
                lastUserProfileId, PAGE_SIZE, scheduledDate);
        if (page.userProfileIds().isEmpty()) {
          recordBatchCompleted(messageId, attemptId, scheduledDate, summary, batchStartedAt);
          return;
        }
        failureStage = "target_selection";
        List<SendPushNotificationCommand> commands =
            pageTransactions.execute(status -> processPage(page, occurredAt));
        recordSelectionMetrics(commands);
        recordUserCounts(page, commands);
        failureStage = "push_dispatch";
        NotificationDispatchResult dispatchResult = notificationDispatchService.sendAll(commands);
        summary = summary.add(page, commands, dispatchResult);
        failureStage = "visibility_extension";
        visibilityExtender.run();
        recordPageCompleted(
            messageId,
            attemptId,
            scheduledDate,
            summary.pageCount(),
            lastUserProfileId,
            page,
            commands,
            dispatchResult,
            pageStartedAt);
        lastUserProfileId = page.userProfileIds().getLast();
      }
    } catch (RuntimeException exception) {
      recordBatchFailed(
          messageId, attemptId, scheduledDate, summary, failureStage, batchStartedAt, exception);
      throw exception;
    }
  }

  /** 완료된 페이지의 처리량과 시간을 로그와 지표로 기록한다. */
  private void recordPageCompleted(
      String messageId,
      String attemptId,
      LocalDate scheduledDate,
      int pageIndex,
      long cursorBefore,
      NotificationTargetPage page,
      List<SendPushNotificationCommand> commands,
      NotificationDispatchResult dispatchResult,
      long pageStartedAt) {
    long durationNanos = System.nanoTime() - pageStartedAt;
    meterRegistry
        .timer("landit.notification.scheduled.page.duration")
        .record(durationNanos, TimeUnit.NANOSECONDS);
    log.info(
        "scheduled_notification_page_completed messageId={} attemptId={} scheduledDate={} "
            + "pageIndex={} cursorBefore={} cursorAfter={} scannedUsers={} sendableUsers={} "
            + "selectedUsers={} preparedDeliveries={} expoRequestCount={} ticketAccepted={} "
            + "ticketFailed={} durationMs={}",
        messageId,
        attemptId,
        scheduledDate,
        pageIndex,
        cursorBefore,
        page.userProfileIds().getLast(),
        page.userProfileIds().size(),
        page.sendableUserProfileIds().size(),
        commands.size(),
        dispatchResult.preparedDeliveries(),
        dispatchResult.expoRequestCount(),
        dispatchResult.ticketAccepted(),
        dispatchResult.ticketFailed(),
        TimeUnit.NANOSECONDS.toMillis(durationNanos));
  }

  /** 성공한 예약 배치의 전체 처리량과 시간을 기록한다. */
  private void recordBatchCompleted(
      String messageId,
      String attemptId,
      LocalDate scheduledDate,
      BatchSummary summary,
      long batchStartedAt) {
    long durationNanos = System.nanoTime() - batchStartedAt;
    recordBatchDuration("success", durationNanos);
    log.info(
        "scheduled_notification_batch_completed messageId={} attemptId={} scheduledDate={} "
            + "policyVersion={} pageCount={} scannedUsers={} sendableUsers={} selectedUsers={} "
            + "preparedDeliveries={} expoRequestCount={} ticketAccepted={} ticketFailed={} "
            + "durationMs={}",
        messageId,
        attemptId,
        scheduledDate,
        POLICY_VERSION,
        summary.pageCount(),
        summary.scannedUsers(),
        summary.sendableUsers(),
        summary.selectedUsers(),
        summary.preparedDeliveries(),
        summary.expoRequestCount(),
        summary.ticketAccepted(),
        summary.ticketFailed(),
        TimeUnit.NANOSECONDS.toMillis(durationNanos));
  }

  /** 실패한 예약 배치의 완료 페이지와 실패 단계를 기록하고 기존 예외를 유지한다. */
  private void recordBatchFailed(
      String messageId,
      String attemptId,
      LocalDate scheduledDate,
      BatchSummary summary,
      String failureStage,
      long batchStartedAt,
      RuntimeException exception) {
    long durationNanos = System.nanoTime() - batchStartedAt;
    recordBatchDuration("failure", durationNanos);
    log.error(
        "scheduled_notification_batch_failed messageId={} attemptId={} scheduledDate={} "
            + "policyVersion={} failureStage={} pageCount={} scannedUsers={} selectedUsers={} "
            + "durationMs={} exception={}",
        messageId,
        attemptId,
        scheduledDate,
        POLICY_VERSION,
        failureStage,
        summary.pageCount(),
        summary.scannedUsers(),
        summary.selectedUsers(),
        TimeUnit.NANOSECONDS.toMillis(durationNanos),
        exception.getClass().getSimpleName(),
        exception);
  }

  /** 예약 배치 시간을 성공 여부별로 기록한다. */
  private void recordBatchDuration(String outcome, long durationNanos) {
    Timer.builder("landit.notification.scheduled.batch.duration")
        .tag("outcome", outcome)
        .register(meterRegistry)
        .record(durationNanos, TimeUnit.NANOSECONDS);
  }

  /** 사용자 조회·Token 보유·알림 선정 건수를 단계별로 기록한다. */
  private void recordUserCounts(
      NotificationTargetPage page, List<SendPushNotificationCommand> commands) {
    incrementUserCount("scanned", page.userProfileIds().size());
    incrementUserCount("sendable", page.sendableUserProfileIds().size());
    incrementUserCount("selected", commands.size());
  }

  /** 사용자 단계별 Counter를 증가시킨다. */
  private void incrementUserCount(String stage, int count) {
    if (count == 0) {
      return;
    }
    meterRegistry.counter("landit.notification.scheduled.users", "stage", stage).increment(count);
  }

  /** 선정된 알림 유형과 문구 변형 건수를 기록한다. */
  private void recordSelectionMetrics(List<SendPushNotificationCommand> commands) {
    commands.forEach(
        command ->
            meterRegistry
                .counter(
                    "landit.notification.selection",
                    "notification_type",
                    command.notificationType().name(),
                    "content_variant",
                    command.contentVariant().name())
                .increment());
  }

  /** 한 페이지의 스냅샷을 저장하고 각 사용자에 대한 실제 발송 명령을 만든다. */
  private List<SendPushNotificationCommand> processPage(
      NotificationTargetPage page, Instant occurredAt) {
    Map<Long, UserNotificationState> statesByUserId = existingStates(page);
    List<SendPushNotificationCommand> commands = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    LocalDate scheduledDate = occurredAt.atZone(KOREA_TIME_ZONE).toLocalDate();
    for (Long userProfileId : page.userProfileIds()) {
      if (!page.sendableUserProfileIds().contains(userProfileId)) {
        continue;
      }
      notificationTargetSelectionService
          .select(page.inputs().get(userProfileId), scheduledDate)
          .ifPresent(
              target -> {
                LocalDateTime latestActivityAt = latestActivityAt(page.inputs().get(userProfileId));
                UserNotificationState state = statesByUserId.get(userProfileId);
                if (state == null) {
                  state =
                      UserNotificationState.ready(
                          userProfileId,
                          target.notificationType(),
                          target.targetId(),
                          latestActivityAt);
                  statesByUserId.put(userProfileId, state);
                } else {
                  state.refresh(target.notificationType(), target.targetId(), latestActivityAt);
                }
                ScheduledNotificationContent content =
                    ScheduledNotificationContent.from(
                        target, page.inputs().get(userProfileId), scheduledDate);
                log.debug(
                    "scheduled_notification_selected userProfileId={} scheduledDate={} "
                        + "notificationType={} contentVariant={}",
                    userProfileId,
                    scheduledDate,
                    target.notificationType(),
                    content.contentVariant());
                commands.add(
                    new SendPushNotificationCommand(
                        eventId(scheduledDate, userProfileId),
                        userProfileId,
                        target.notificationType(),
                        content.contentVariant(),
                        content.title(),
                        content.body(),
                        content.deepLink()));
                state.markSent(now);
              });
    }
    userNotificationStateRepository.saveAll(statesByUserId.values());
    return commands;
  }

  /** 기존 상태를 사용자 ID별로 묶어 행마다 추가 조회하지 않도록 준비한다. */
  private Map<Long, UserNotificationState> existingStates(NotificationTargetPage page) {
    Map<Long, UserNotificationState> statesByUserId = new HashMap<>();
    userNotificationStateRepository
        .findAllByUserProfileIdIn(page.userProfileIds())
        .forEach(state -> statesByUserId.put(state.getUserProfileId(), state));
    return statesByUserId;
  }

  /** 시나리오·표현 완료 시각 중 최신 값을 스냅샷에 기록한다. */
  private LocalDateTime latestActivityAt(NotificationTargetSelectionInput input) {
    if (input.lastScenarioCompletedAt() == null) {
      return input.lastExpressionCompletedAt();
    }
    if (input.lastExpressionCompletedAt() == null
        || !input.lastScenarioCompletedAt().isBefore(input.lastExpressionCompletedAt())) {
      return input.lastScenarioCompletedAt();
    }
    return input.lastExpressionCompletedAt();
  }

  /** 날짜·사용자 조합으로 하루 한 건을 보장하는 설치별 push_delivery 공통 이벤트 ID를 만든다. */
  private String eventId(LocalDate scheduledDate, Long userProfileId) {
    return "scheduled:" + scheduledDate + ":" + userProfileId;
  }

  /** 완료된 페이지의 예약 알림 처리량을 누적한다. */
  private record BatchSummary(
      int pageCount,
      int scannedUsers,
      int sendableUsers,
      int selectedUsers,
      int preparedDeliveries,
      int expoRequestCount,
      int ticketAccepted,
      int ticketFailed) {

    /** 빈 배치 집계를 반환한다. */
    private static BatchSummary empty() {
      return new BatchSummary(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** 완료된 페이지와 발송 결과를 누적한 새 집계를 반환한다. */
    private BatchSummary add(
        NotificationTargetPage page,
        List<SendPushNotificationCommand> commands,
        NotificationDispatchResult dispatchResult) {
      return new BatchSummary(
          pageCount + 1,
          scannedUsers + page.userProfileIds().size(),
          sendableUsers + page.sendableUserProfileIds().size(),
          selectedUsers + commands.size(),
          preparedDeliveries + dispatchResult.preparedDeliveries(),
          expoRequestCount + dispatchResult.expoRequestCount(),
          ticketAccepted + dispatchResult.ticketAccepted(),
          ticketFailed + dispatchResult.ticketFailed());
    }
  }
}
