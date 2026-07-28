// 사용자별 시나리오 복습 권한을 조회하고 저장한다.

package com.landit.landitbe.feature.learning.repository;

import com.landit.landitbe.feature.learning.domain.UserScenarioAccess;
import com.landit.landitbe.shared.domain.Locale;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자별 시나리오 복습 권한을 조회하고 저장한다. */
public interface UserScenarioAccessRepository extends JpaRepository<UserScenarioAccess, Long> {

  /**
   * 사용자가 대상 언어의 특정 시나리오 복습 권한을 보유하는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @return 복습 권한 보유 여부
   */
  boolean existsByUserProfileIdAndScenarioIdAndTargetLocale(
      Long userProfileId, Long scenarioId, Locale targetLocale);

  /**
   * 사용자가 학습 대상 언어로 보유한 모든 시나리오 복습 권한을 시나리오 ID 순서로 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param targetLocale 학습 대상 언어
   * @return 시나리오 복습 권한 목록
   */
  List<UserScenarioAccess> findAllByUserProfileIdAndTargetLocaleOrderByScenarioIdAsc(
      Long userProfileId, Locale targetLocale);
}
