// 사용자 시나리오 진행도 엔티티의 조회와 저장을 담당한다.

package com.landit.landitbe.feature.learning.repository;

import com.landit.landitbe.feature.learning.domain.UserScenarioProgress;
import com.landit.landitbe.feature.learning.domain.UserScenarioProgressStatus;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자 시나리오 진행도 엔티티의 조회와 저장을 담당한다. */
public interface UserScenarioProgressRepository extends JpaRepository<UserScenarioProgress, Long> {

  /** 사용자, 시나리오, 학습 locale 조합의 진행도를 조회한다. */
  Optional<UserScenarioProgress> findByUserProfileIdAndScenarioIdAndTargetLocale(
      Long userProfileId, Long scenarioId, Locale targetLocale);

  /**
   * 특정 시각 이후에 마지막으로 완료한 특정 상태의 진행도가 있는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param status 확인할 진행 상태
   * @param lastClearedAt 이 시각 이상으로 완료한 진행도만 센다
   * @return 조건에 맞는 진행도가 하나라도 있으면 {@code true}
   */
  boolean existsByUserProfileIdAndStatusAndLastClearedAtGreaterThanEqual(
      Long userProfileId, UserScenarioProgressStatus status, LocalDateTime lastClearedAt);
}
