// 사용자별 학습 알림 대상 선정에 필요한 일괄 조회 결과를 전달한다.

package com.landit.landitbe.feature.notification.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자별 학습 알림 대상 선정에 필요한 일괄 조회 결과다.
 *
 * @param userProfileId 사용자 프로필 ID
 * @param nickname 사용자 닉네임
 * @param dailyScenarioId 예약 날짜에 배정된 시나리오 ID
 * @param dailyScenarioCompleted 예약 날짜의 시나리오 완료 여부
 * @param freeTalkUsedSpeakingDurationMs 예약 날짜의 스몰톡 사용 시간
 * @param activeToday 예약 날짜의 스트릭 달성 여부
 * @param activeYesterday 예약 날짜 전날의 스트릭 달성 여부
 * @param currentStreakDays 현재 연속 학습일
 * @param longestStreakDays 최장 연속 학습일
 * @param priorActiveDayHistory 예약 날짜 이전 스트릭 달성 이력 존재 여부
 * @param missedDayCount 마지막 스트릭 달성일 이후 예약 날짜 전날까지의 미학습 일수
 * @param latestFreeTalkTitle 가장 최근 스몰톡 제목
 * @param lastScenarioCompletedAt 가장 최근 시나리오 완료 시각
 * @param lastExpressionCompletedAt 가장 최근 표현 완료 시각
 * @param expressions 예약 날짜 시나리오의 표현 후보
 */
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
