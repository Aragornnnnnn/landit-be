// 사용자별 학습 알림 대상 선정에 필요한 일괄 조회 결과를 전달한다.

package com.landit.landitbe.feature.notification.service;

import java.time.LocalDateTime;
import java.util.List;

record NotificationTargetSelectionInput(
    Long userProfileId,
    String nickname,
    Long dailyScenarioId,
    boolean dailyScenarioCompleted,
    long freeTalkUsedSpeakingDurationMs,
    boolean activeToday,
    boolean activeYesterday,
    Integer currentStreakDays,
    Integer longestStreakDays,
    boolean priorActiveDayHistory,
    Integer missedDayCount,
    String latestFreeTalkTitle,
    LocalDateTime lastScenarioCompletedAt,
    LocalDateTime lastExpressionCompletedAt,
    List<ExpressionNotificationCandidate> expressions) {

  NotificationTargetSelectionInput(
      Long userProfileId,
      Long dailyScenarioId,
      boolean dailyScenarioCompleted,
      long freeTalkUsedSpeakingDurationMs,
      LocalDateTime lastScenarioCompletedAt,
      LocalDateTime lastExpressionCompletedAt,
      List<ExpressionNotificationCandidate> expressions) {
    this(
        userProfileId,
        null,
        dailyScenarioId,
        dailyScenarioCompleted,
        freeTalkUsedSpeakingDurationMs,
        false,
        false,
        null,
        null,
        false,
        null,
        null,
        lastScenarioCompletedAt,
        lastExpressionCompletedAt,
        expressions);
  }

  NotificationTargetSelectionInput(
      Long userProfileId,
      String nickname,
      Long dailyScenarioId,
      boolean dailyScenarioCompleted,
      long freeTalkUsedSpeakingDurationMs,
      boolean activeToday,
      boolean activeYesterday,
      Integer currentStreakDays,
      Integer longestStreakDays,
      boolean priorActiveDayHistory,
      Integer missedDayCount,
      String latestFreeTalkTitle,
      List<ExpressionNotificationCandidate> expressions) {
    this(
        userProfileId,
        nickname,
        dailyScenarioId,
        dailyScenarioCompleted,
        freeTalkUsedSpeakingDurationMs,
        activeToday,
        activeYesterday,
        currentStreakDays,
        longestStreakDays,
        priorActiveDayHistory,
        missedDayCount,
        latestFreeTalkTitle,
        null,
        null,
        expressions);
  }
}
