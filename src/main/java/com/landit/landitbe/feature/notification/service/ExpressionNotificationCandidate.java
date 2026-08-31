// 사용자별 표현 이어 하기 후보의 완료 상태와 개인화 원문을 전달한다.

package com.landit.landitbe.feature.notification.service;

record ExpressionNotificationCandidate(
    Long scenarioId, Long expressionId, boolean completed, String targetExpressionText) {

  ExpressionNotificationCandidate(Long scenarioId, Long expressionId, boolean completed) {
    this(scenarioId, expressionId, completed, null);
  }
}
