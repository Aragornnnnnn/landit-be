// 사용자별 학습 알림 대상 선정에 필요한 일괄 조회 결과를 전달한다.

package com.landit.landitbe.feature.notification.service;

import java.time.LocalDateTime;
import java.util.List;

record NotificationTargetSelectionInput(
    Long userProfileId,
    LocalDateTime lastScenarioCompletedAt,
    Long lastScenarioId,
    LocalDateTime lastExpressionCompletedAt,
    Long lastExpressionScenarioId,
    List<ScenarioNotificationCandidate> scenarios,
    List<ExpressionNotificationCandidate> expressions) {}
