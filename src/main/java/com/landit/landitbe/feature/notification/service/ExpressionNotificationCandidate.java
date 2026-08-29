// 사용자별 표현 이어 하기 후보의 부모 완료 상태를 전달한다.

package com.landit.landitbe.feature.notification.service;

record ExpressionNotificationCandidate(Long scenarioId, Long expressionId, boolean completed) {}
