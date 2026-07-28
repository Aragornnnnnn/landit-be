// 사용자 시나리오 복습 권한의 보유 여부와 획득 처리를 담당한다.

package com.landit.landitbe.feature.learning.service;

import com.landit.landitbe.feature.learning.domain.UserScenarioAccess;
import com.landit.landitbe.feature.learning.repository.UserScenarioAccessRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 시나리오 복습 권한의 보유 여부와 획득 처리를 담당한다. */
@Service
@RequiredArgsConstructor
public class ScenarioAccessService {

  private final UserScenarioAccessRepository userScenarioAccessRepository;
  private final UserProfileService userProfileService;

  /**
   * 사용자가 대상 언어의 특정 시나리오 복습 권한을 보유하는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @return 복습 권한 보유 여부
   */
  @Transactional(readOnly = true)
  public boolean hasAccess(Long userProfileId, Long scenarioId, Locale targetLocale) {
    return userScenarioAccessRepository.existsByUserProfileIdAndScenarioIdAndTargetLocale(
        userProfileId, scenarioId, targetLocale);
  }

  /**
   * 사용자가 대상 언어로 복습할 수 있는 시나리오 ID 목록을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @return 복습 가능한 시나리오 ID 목록
   */
  @Transactional(readOnly = true)
  public List<Long> findAccessibleScenarioIds(Long userProfileId, Locale targetLocale) {
    return userScenarioAccessRepository
        .findAllByUserProfileIdAndTargetLocaleOrderByScenarioIdAsc(userProfileId, targetLocale)
        .stream()
        .map(UserScenarioAccess::getScenarioId)
        .toList();
  }

  /**
   * 시나리오 완료로 복습 권한을 획득한다. 이미 보유한 권한이면 성공으로 처리한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 완료한 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @param grantedAt 복습 권한을 얻은 시각
   * @throws ApiException 사용자 프로필이 없거나 비활성 상태일 때
   */
  @Transactional
  public void grantAccess(
      Long userProfileId, Long scenarioId, Locale targetLocale, LocalDateTime grantedAt) {
    userProfileService.requireActiveForUpdate(userProfileId);
    if (hasAccess(userProfileId, scenarioId, targetLocale)) {
      return;
    }
    userScenarioAccessRepository.save(
        UserScenarioAccess.grant(userProfileId, scenarioId, targetLocale, grantedAt));
  }
}
