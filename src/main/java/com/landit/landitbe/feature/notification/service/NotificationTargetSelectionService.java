// 학습 완료 이력과 현재 콘텐츠 상태로 사용자별 알림 대상 하나를 선정한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** 학습 완료 이력과 현재 콘텐츠 상태로 사용자별 알림 대상 하나를 선정한다. */
@Service
public class NotificationTargetSelectionService {

  /**
   * 사용자 한 명의 현재 콘텐츠와 마지막 완료 이력에서 하루 알림 대상 하나를 선정한다.
   *
   * @param input 사용자별 일괄 조회 결과
   * @return 발송할 알림 대상. 아직 완료하지 않은 콘텐츠가 없고 완주 상태도 아니면 비어 있다.
   */
  public Optional<SelectedNotificationTarget> select(NotificationTargetSelectionInput input) {
    List<ScenarioNotificationCandidate> scenarios =
        accessibleIncompleteScenarios(input.scenarios());
    List<ExpressionNotificationCandidate> expressions =
        incompleteEligibleExpressions(input.expressions());
    if (!hasCompletionHistory(input)) {
      return scenarios.stream().findFirst().map(this::scenarioTarget);
    }

    boolean scenarioFirst = scenarioCompletedMostRecently(input);
    Optional<SelectedNotificationTarget> preferred =
        scenarioFirst
            ? scenarioInLatestCategory(input, scenarios)
            : expressionInLatestScenario(input, expressions);
    if (preferred.isPresent()) {
      return preferred;
    }

    Optional<SelectedNotificationTarget> fallback =
        scenarioFirst
            ? expressions.stream().findFirst().map(this::expressionTarget)
            : scenarios.stream().findFirst().map(this::scenarioTarget);
    if (fallback.isPresent()) {
      return fallback;
    }
    return allCurrentContentCompleted(input)
        ? Optional.of(new SelectedNotificationTarget(NotificationType.REVIEW_LEARNING, null))
        : Optional.empty();
  }

  /** 기존 화면과 같은 카테고리별 순차 잠금 규칙을 적용한다. */
  private List<ScenarioNotificationCandidate> accessibleIncompleteScenarios(
      List<ScenarioNotificationCandidate> scenarios) {
    Map<Long, Boolean> previousCompletedByCategory = new HashMap<>();
    return scenarios.stream()
        .filter(
            scenario -> {
              boolean previousCompleted =
                  previousCompletedByCategory.getOrDefault(scenario.categoryId(), true);
              previousCompletedByCategory.put(scenario.categoryId(), scenario.cleared());
              return scenario.categoryActive()
                  && scenario.scenarioActive()
                  && scenario.variantActive()
                  && previousCompleted
                  && !scenario.cleared();
            })
        .toList();
  }

  /** 완료한 부모 시나리오에 속한 미완료 활성 표현만 이어 하기 후보로 남긴다. */
  private List<ExpressionNotificationCandidate> incompleteEligibleExpressions(
      List<ExpressionNotificationCandidate> expressions) {
    return expressions.stream()
        .filter(expression -> expression.parentScenarioCleared() && !expression.completed())
        .toList();
  }

  /** 완료 이력이 전혀 없는 신규 사용자인지 확인한다. */
  private boolean hasCompletionHistory(NotificationTargetSelectionInput input) {
    return input.lastScenarioCompletedAt() != null || input.lastExpressionCompletedAt() != null;
  }

  /** 동률에서는 시나리오 완료를 우선해 결정적인 유형 선택을 보장한다. */
  private boolean scenarioCompletedMostRecently(NotificationTargetSelectionInput input) {
    if (input.lastExpressionCompletedAt() == null) {
      return true;
    }
    if (input.lastScenarioCompletedAt() == null) {
      return false;
    }
    return !input.lastScenarioCompletedAt().isBefore(input.lastExpressionCompletedAt());
  }

  /** 최근 시나리오 완료와 같은 카테고리의 후보를 우선한다. */
  private Optional<SelectedNotificationTarget> scenarioInLatestCategory(
      NotificationTargetSelectionInput input, List<ScenarioNotificationCandidate> scenarios) {
    return scenarios.stream()
        .filter(scenario -> scenario.categoryId().equals(categoryIdOfLatestScenario(input)))
        .findFirst()
        .map(this::scenarioTarget)
        .or(() -> scenarios.stream().findFirst().map(this::scenarioTarget));
  }

  /** 최근 표현 완료와 같은 부모 시나리오의 후보를 우선한다. */
  private Optional<SelectedNotificationTarget> expressionInLatestScenario(
      NotificationTargetSelectionInput input, List<ExpressionNotificationCandidate> expressions) {
    return expressions.stream()
        .filter(expression -> expression.scenarioId().equals(input.lastExpressionScenarioId()))
        .findFirst()
        .map(this::expressionTarget)
        .or(() -> expressions.stream().findFirst().map(this::expressionTarget));
  }

  /** 최근 완료한 시나리오의 카테고리 ID를 조회 결과에서 찾는다. */
  private Long categoryIdOfLatestScenario(NotificationTargetSelectionInput input) {
    return input.scenarios().stream()
        .filter(scenario -> scenario.scenarioId().equals(input.lastScenarioId()))
        .findFirst()
        .map(ScenarioNotificationCandidate::categoryId)
        .orElse(null);
  }

  /** 현재 활성 시나리오와 사용자 언어의 활성 표현이 모두 완료됐는지 확인한다. */
  private boolean allCurrentContentCompleted(NotificationTargetSelectionInput input) {
    if (input.scenarios().isEmpty() && input.expressions().isEmpty()) {
      return false;
    }
    return input.scenarios().stream()
            .filter(
                scenario ->
                    scenario.categoryActive()
                        && scenario.scenarioActive()
                        && scenario.variantActive())
            .allMatch(ScenarioNotificationCandidate::cleared)
        && input.expressions().stream().allMatch(ExpressionNotificationCandidate::completed);
  }

  /** 시나리오 후보를 발송 대상 값으로 변환한다. */
  private SelectedNotificationTarget scenarioTarget(ScenarioNotificationCandidate scenario) {
    return new SelectedNotificationTarget(
        NotificationType.CONTINUE_SCENARIO, scenario.scenarioId());
  }

  /** 표현 후보를 발송 대상 값으로 변환한다. */
  private SelectedNotificationTarget expressionTarget(ExpressionNotificationCandidate expression) {
    return new SelectedNotificationTarget(
        NotificationType.CONTINUE_EXPRESSION, expression.expressionId());
  }
}
