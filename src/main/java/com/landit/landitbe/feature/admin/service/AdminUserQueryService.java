// 관리자 사용자 목록과 상세 조회를 조합한다.

package com.landit.landitbe.feature.admin.service;

import com.landit.landitbe.feature.admin.dto.AdminUserDetailResponse;
import com.landit.landitbe.feature.admin.dto.AdminUserListResponse;
import com.landit.landitbe.feature.character.service.StreakService;
import com.landit.landitbe.feature.content.service.ScenarioProgressionService;
import com.landit.landitbe.feature.content.service.ScenarioQueryService;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.dto.AdminUserProfile;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 사용자 목록과 상세 조회를 조합한다. */
@Service
@RequiredArgsConstructor
public class AdminUserQueryService {

  private final UserProfileService userProfileService;
  private final ScenarioAccessService scenarioAccessService;
  private final StreakService streakService;
  private final ScenarioProgressionService scenarioProgressionService;
  private final ScenarioQueryService scenarioQueryService;
  private final Clock clock;

  /**
   * 관리자 사용자 목록을 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 관리자 사용자 목록 응답
   */
  @Transactional(readOnly = true)
  public AdminUserListResponse getUsers(int page, int size) {
    return AdminUserListResponse.from(userProfileService.getAdminUserProfiles(page, size));
  }

  /**
   * 사용자 프로필과 학습 요약을 관리자 상세 응답으로 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 관리자 사용자 상세 응답
   * @throws UserProfileException 사용자가 없을 때
   */
  @Transactional(readOnly = true)
  public AdminUserDetailResponse getUser(long userProfileId) {
    AdminUserProfile profile = userProfileService.getAdminUserProfile(userProfileId);

    long completedScenarioCount =
        scenarioAccessService.countCompletedScenarios(userProfileId, profile.targetLocale());
    StreakService.LearningActivitySummary activitySummary =
        streakService.getLearningActivitySummary(userProfileId);

    // 비활성 사용자는 더 이상 학습 대상이 아니므로
    // 현재 제공 시나리오를 계산하지 않는다.
    AdminUserDetailResponse.CurrentScenario currentScenario =
        profile.status() == UserProfileStatus.ACTIVE ? findCurrentScenario(profile) : null;

    return AdminUserDetailResponse.from(
        profile, completedScenarioCount, currentScenario, activitySummary);
  }

  /**
   * 활성 사용자에게만 기존 순차 진행 정책으로 현재 시나리오를 계산한다.
   *
   * @param profile 관리자 사용자 프로필
   * @return 현재 제공 대상 시나리오. 제공할 시나리오가 없으면 null
   */
  private AdminUserDetailResponse.CurrentScenario findCurrentScenario(AdminUserProfile profile) {
    ScenarioProgressionService.CurrentScenario currentScenario =
        scenarioProgressionService
            .findCurrentScenario(profile.userProfileId(), profile.targetLocale(), clock.instant())
            .orElse(null);
    if (currentScenario == null) {
      return null;
    }

    return scenarioQueryService
        .findScenarioSummary(profile.userProfileId(), currentScenario.scenarioId())
        .map(
            summary ->
                AdminUserDetailResponse.CurrentScenario.from(summary, currentScenario.type()))
        .orElse(null);
  }
}
