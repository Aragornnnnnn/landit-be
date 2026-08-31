// 오늘 배정된 시나리오와 스몰톡 사용량으로 사용자별 예약 알림 대상을 선정한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import java.time.LocalDate;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 오늘 배정된 시나리오와 스몰톡 사용량으로 사용자별 예약 알림 대상을 선정한다. */
@Slf4j
@Service
public class NotificationTargetSelectionService {

  /**
   * 오늘의 학습 상태에서 정책 우선순위가 가장 높은 예약 알림 하나를 선정한다.
   *
   * @param input 사용자별 일괄 조회 결과
   * @return 발송할 알림 대상. 오늘의 시나리오를 결정할 수 없거나 모든 알림 조건이 소진되면 빈 값
   */
  public Optional<SelectedNotificationTarget> select(NotificationTargetSelectionInput input) {
    return selectWithExpressionPriority(input);
  }

  /** 예약 날짜를 포함한 학습 상태에서 결정적으로 예약 알림 하나를 선정한다. */
  public Optional<SelectedNotificationTarget> select(
      NotificationTargetSelectionInput input, LocalDate scheduledDate) {
    if (input.dailyScenarioId() == null) {
      log.warn(
          "scheduled_notification_skipped reason=daily_scenario_unavailable userProfileId={}",
          input.userProfileId());
      return Optional.empty();
    }
    if (!input.dailyScenarioCompleted()) {
      return Optional.of(
          new SelectedNotificationTarget(
              NotificationType.DAILY_SCENARIO_REMINDER, input.dailyScenarioId(), null));
    }
    Optional<ExpressionNotificationCandidate> incompleteExpression =
        input.expressions().stream()
            .filter(
                expression ->
                    expression.scenarioId().equals(input.dailyScenarioId())
                        && !expression.completed())
            .findFirst();
    boolean expressionEligible = incompleteExpression.isPresent();
    boolean smallTalkEligible = input.freeTalkUsedSpeakingDurationMs() == 0L;
    if (expressionEligible && smallTalkEligible) {
      int selectedIndex =
          DeterministicNotificationChoice.choose(
              "scheduled-notification:type:v1", scheduledDate, input.userProfileId(), 2);
      if (selectedIndex == 1) {
        return Optional.of(
            new SelectedNotificationTarget(NotificationType.SMALL_TALK_REMINDER, null, null));
      }
    }
    if (expressionEligible) {
      ExpressionNotificationCandidate expression = incompleteExpression.get();
      return Optional.of(
          new SelectedNotificationTarget(
              NotificationType.CONTINUE_EXPRESSION,
              expression.expressionId(),
              expression.scenarioId()));
    }
    if (smallTalkEligible) {
      return Optional.of(
          new SelectedNotificationTarget(NotificationType.SMALL_TALK_REMINDER, null, null));
    }
    return Optional.empty();
  }

  /** 기존 날짜 없는 호출자의 표현 우선순위 계약을 유지한다. */
  private Optional<SelectedNotificationTarget> selectWithExpressionPriority(
      NotificationTargetSelectionInput input) {
    if (input.dailyScenarioId() == null) {
      log.warn(
          "scheduled_notification_skipped reason=daily_scenario_unavailable userProfileId={}",
          input.userProfileId());
      return Optional.empty();
    }
    if (!input.dailyScenarioCompleted()) {
      return Optional.of(
          new SelectedNotificationTarget(
              NotificationType.DAILY_SCENARIO_REMINDER, input.dailyScenarioId(), null));
    }
    Optional<ExpressionNotificationCandidate> incompleteExpression =
        input.expressions().stream()
            .filter(
                expression ->
                    expression.scenarioId().equals(input.dailyScenarioId())
                        && !expression.completed())
            .findFirst();
    if (incompleteExpression.isPresent()) {
      ExpressionNotificationCandidate expression = incompleteExpression.get();
      return Optional.of(
          new SelectedNotificationTarget(
              NotificationType.CONTINUE_EXPRESSION,
              expression.expressionId(),
              expression.scenarioId()));
    }
    if (input.freeTalkUsedSpeakingDurationMs() == 0L) {
      return Optional.of(
          new SelectedNotificationTarget(NotificationType.SMALL_TALK_REMINDER, null, null));
    }
    return Optional.empty();
  }
}
