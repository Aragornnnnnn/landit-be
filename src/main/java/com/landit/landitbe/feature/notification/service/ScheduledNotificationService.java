// 매일 예약된 학습 알림 대상을 페이지 단위로 계산하고 직접 발송한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.UserNotificationState;
import com.landit.landitbe.feature.notification.repository.UserNotificationStateRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** 매일 예약된 학습 알림 대상을 페이지 단위로 계산하고 직접 발송한다. */
@Service
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class ScheduledNotificationService {

  private static final int PAGE_SIZE = 500;
  private static final ZoneId KOREA_TIME_ZONE = ZoneId.of("Asia/Seoul");

  private final NotificationTargetPageQueryService notificationTargetPageQueryService;
  private final NotificationTargetSelectionService notificationTargetSelectionService;
  private final UserNotificationStateRepository userNotificationStateRepository;
  private final NotificationDispatchService notificationDispatchService;
  private final TransactionOperations pageTransactions;

  /**
   * 페이지별 계산과 상태 저장을 독립 트랜잭션으로 실행하도록 서비스를 구성한다.
   *
   * @param notificationTargetPageQueryService 대상 계산용 일괄 조회 Service
   * @param notificationTargetSelectionService 사용자별 대상 선정 Service
   * @param userNotificationStateRepository 알림 상태 저장소
   * @param notificationDispatchService 페이지 단위 Push 발송 Service
   * @param transactionManager 애플리케이션 트랜잭션 관리자
   */
  public ScheduledNotificationService(
      NotificationTargetPageQueryService notificationTargetPageQueryService,
      NotificationTargetSelectionService notificationTargetSelectionService,
      UserNotificationStateRepository userNotificationStateRepository,
      NotificationDispatchService notificationDispatchService,
      PlatformTransactionManager transactionManager) {
    this.notificationTargetPageQueryService = notificationTargetPageQueryService;
    this.notificationTargetSelectionService = notificationTargetSelectionService;
    this.userNotificationStateRepository = userNotificationStateRepository;
    this.notificationDispatchService = notificationDispatchService;
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.pageTransactions = transactionTemplate;
  }

  /**
   * Scheduler 기준 시각으로 모든 활성 사용자를 500명씩 계산해 페이지 단위로 직접 발송한다.
   *
   * @param occurredAt EventBridge Scheduler가 지정한 예정 시각
   * @param visibilityExtender 현재 SQS 메시지의 visibility를 연장하는 작업
   */
  public void process(Instant occurredAt, Runnable visibilityExtender) {
    long lastUserProfileId = 0L;
    LocalDate scheduledDate = occurredAt.atZone(KOREA_TIME_ZONE).toLocalDate();
    while (true) {
      visibilityExtender.run();
      NotificationTargetPage page =
          notificationTargetPageQueryService.loadPage(lastUserProfileId, PAGE_SIZE, scheduledDate);
      if (page.userProfileIds().isEmpty()) {
        return;
      }
      List<SendPushNotificationCommand> commands =
          pageTransactions.execute(status -> processPage(page, occurredAt));
      notificationDispatchService.sendAll(commands);
      visibilityExtender.run();
      lastUserProfileId = page.userProfileIds().getLast();
    }
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
          .select(page.inputs().get(userProfileId))
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
                ScheduledNotificationContent content = ScheduledNotificationContent.from(target);
                commands.add(
                    new SendPushNotificationCommand(
                        eventId(scheduledDate, userProfileId),
                        userProfileId,
                        target.notificationType(),
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
}
