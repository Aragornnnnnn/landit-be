// 관리자 사용자 상세와 학습 요약 응답을 정의한다.

package com.landit.landitbe.feature.admin.dto;

import com.landit.landitbe.feature.character.service.StreakService;
import com.landit.landitbe.feature.content.domain.DailyScenarioType;
import com.landit.landitbe.feature.content.service.ScenarioQueryService;
import com.landit.landitbe.feature.profile.domain.LearningLevel;
import com.landit.landitbe.feature.profile.domain.PushPermissionStatus;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.feature.profile.dto.AdminUserProfile;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 관리자 사용자 상세와 학습 요약 응답을 정의한다.
 *
 * @param userProfileId 사용자 프로필 ID
 * @param email 이메일
 * @param nickname 닉네임
 * @param role 사용자 역할
 * @param status 계정 상태
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 기준 언어
 * @param learningLevel 학습 레벨
 * @param currentLevel 현재 레벨
 * @param aiTutorId AI 튜터 ID
 * @param pushPermissionStatus 푸시 권한 상태
 * @param createdAt 가입 시각
 * @param updatedAt 수정 시각
 * @param learningSummary 학습 요약
 */
public record AdminUserDetailResponse(
    Long userProfileId,
    String email,
    String nickname,
    UserRole role,
    UserProfileStatus status,
    Locale targetLocale,
    Locale baseLocale,
    LearningLevel learningLevel,
    int currentLevel,
    Long aiTutorId,
    PushPermissionStatus pushPermissionStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LearningSummary learningSummary) {

  /**
   * 프로필과 학습 조회 결과를 관리자 상세 응답으로 조립한다.
   *
   * @param profile 사용자 프로필
   * @param completedScenarioCount 완료한 시나리오 수
   * @param currentScenario 현재 제공 대상 시나리오
   * @param activitySummary 학습 활동 요약
   * @return 관리자 사용자 상세 응답
   */
  public static AdminUserDetailResponse from(
      AdminUserProfile profile,
      long completedScenarioCount,
      CurrentScenario currentScenario,
      StreakService.LearningActivitySummary activitySummary) {
    LearningSummary learningSummary =
        new LearningSummary(
            completedScenarioCount,
            currentScenario,
            activitySummary.currentStreakDays(),
            activitySummary.lastActivityDate());

    return new AdminUserDetailResponse(
        profile.userProfileId(),
        profile.email(),
        profile.nickname(),
        profile.role(),
        profile.status(),
        profile.targetLocale(),
        profile.baseLocale(),
        profile.learningLevel(),
        profile.currentLevel(),
        profile.aiTutorId(),
        profile.pushPermissionStatus(),
        profile.createdAt(),
        profile.updatedAt(),
        learningSummary);
  }

  /**
   * 관리자 사용자 상세에 포함할 학습 요약이다.
   *
   * @param completedScenarioCount 완료한 시나리오 수
   * @param currentScenario 현재 제공 대상 시나리오
   * @param currentStreakDays 현재 스트릭 일수
   * @param lastLearningDate 최근 학습일
   */
  public record LearningSummary(
      long completedScenarioCount,
      CurrentScenario currentScenario,
      int currentStreakDays,
      LocalDate lastLearningDate) {}

  /**
   * 현재 제공 대상 시나리오 정보다.
   *
   * @param scenarioId 시나리오 ID
   * @param scenarioTitle 시나리오 제목
   * @param displayOrder 시나리오 노출 순서
   * @param dailyScenarioType 데일리 시나리오 유형
   */
  public record CurrentScenario(
      Long scenarioId,
      String scenarioTitle,
      int displayOrder,
      DailyScenarioType dailyScenarioType) {

    /**
     * 시나리오 Service 공개 계약을 상세 응답으로 변환한다.
     *
     * @param summary 시나리오 기본 정보
     * @param dailyScenarioType 데일리 시나리오 유형
     * @return 현재 제공 대상 시나리오 응답
     */
    public static CurrentScenario from(
        ScenarioQueryService.ScenarioSummary summary, DailyScenarioType dailyScenarioType) {
      return new CurrentScenario(
          summary.scenarioId(), summary.scenarioTitle(), summary.displayOrder(), dailyScenarioType);
    }
  }
}
