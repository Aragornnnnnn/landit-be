// 사용자별 세션 수준 평가 이력의 조회와 저장을 담당한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.UserLevelAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자별 세션 수준 평가 이력의 조회와 저장을 담당한다. */
public interface UserLevelAssessmentRepository extends JpaRepository<UserLevelAssessment, Long> {

  /**
   * 학습 세션 식별자로 수준 평가 이력을 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 해당 세션의 수준 평가 이력
   */
  Optional<UserLevelAssessment> findByLearningSessionId(Long learningSessionId);
}
