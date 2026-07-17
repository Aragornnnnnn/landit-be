// 사용자 시나리오 진행도 엔티티의 조회와 저장을 담당한다.

package com.landit.landitbe.learning.infrastructure;

import com.landit.landitbe.common.domain.Locale;
import com.landit.landitbe.learning.domain.UserScenarioProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자 시나리오 진행도 엔티티의 조회와 저장을 담당한다. */
public interface UserScenarioProgressRepository extends JpaRepository<UserScenarioProgress, Long> {

  /** 사용자, 시나리오, 학습 locale 조합의 진행도를 조회한다. */
  Optional<UserScenarioProgress> findByUserProfileIdAndScenarioIdAndTargetLocale(
      Long userProfileId, Long scenarioId, Locale targetLocale);
}
