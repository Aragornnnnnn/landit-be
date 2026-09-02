// 사용자별 표현 이어 하기 후보의 완료 상태와 개인화 원문을 전달한다.

package com.landit.landitbe.feature.notification.service;

/**
 * 사용자별 표현 이어 하기 후보의 완료 상태와 개인화 원문이다.
 *
 * @param scenarioId 표현이 속한 시나리오 ID
 * @param expressionId 표현 ID
 * @param completed 사용자의 표현 완료 여부
 * @param targetExpressionText 알림 개인화에 사용하는 대표 표현 원문
 */
record ExpressionNotificationCandidate(
    Long scenarioId, Long expressionId, boolean completed, String targetExpressionText) {

  ExpressionNotificationCandidate(Long scenarioId, Long expressionId, boolean completed) {
    this(scenarioId, expressionId, completed, null);
  }
}
