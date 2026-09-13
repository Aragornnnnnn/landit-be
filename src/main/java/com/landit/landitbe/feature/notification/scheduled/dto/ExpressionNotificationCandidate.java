// 사용자별 표현 이어 하기 후보의 완료 상태와 개인화 원문을 전달한다.

package com.landit.landitbe.feature.notification.scheduled.dto;

/**
 * 사용자별 표현 이어 하기 후보의 완료 상태와 개인화 원문이다.
 *
 * @param scenarioId 표현이 속한 시나리오 ID
 * @param expressionId 표현 ID
 * @param completed 사용자의 표현 완료 여부
 * @param targetExpressionText 알림 개인화에 사용하는 대표 표현 원문
 */
public record ExpressionNotificationCandidate(
    Long scenarioId, Long expressionId, boolean completed, String targetExpressionText) {

  /**
   * 제공된 학습 상태로 알림 조회 값을 생성한다.
   *
   * @param scenarioId 표현이 속한 시나리오 ID
   * @param expressionId 표현 ID
   * @param completed 사용자의 표현 완료 여부
   */
  public ExpressionNotificationCandidate(Long scenarioId, Long expressionId, boolean completed) {
    this(scenarioId, expressionId, completed, null);
  }
}
