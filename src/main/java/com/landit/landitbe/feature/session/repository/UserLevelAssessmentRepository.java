// 사용자별 세션 수준 평가 이력의 조회와 저장을 담당한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.UserLevelAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자별 세션 수준 평가 이력의 조회와 저장을 담당한다. */
public interface UserLevelAssessmentRepository extends JpaRepository<UserLevelAssessment, Long> {

  /**
   * 학습 세션 식별자로 수준 평가 이력을 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 해당 세션의 수준 평가 이력
   */
  Optional<UserLevelAssessment> findByLearningSessionId(Long learningSessionId);

  /**
   * 평가가 실제로 수준을 확정한 이력이 있는지 확인한다.
   *
   * <p>기존 INITIALIZED/PROMOTED와 v1.2 즉시 적용 결과를 보존한다. v1.1의 자가선택 수준을 유지한 UNCHANGED는 최초 확정으로 간주하지
   * 않는다.
   *
   * @param userProfileId 평가 대상 사용자 ID
   * @return 평가로 수준이 이미 확정됐으면 true
   */
  @Query(
      value =
          """
          SELECT EXISTS (
              SELECT 1 FROM user_level_assessment
              WHERE user_profile_id = :userProfileId AND source = 'MODEL'
                AND (change_type IN ('INITIALIZED', 'PROMOTED', 'DEMOTED')
                  OR (assessment_version = 'text-level-v1.2'
                    AND change_type = 'UNCHANGED' AND sufficient_evidence = TRUE))
          )
          """,
      nativeQuery = true)
  boolean existsInitializedLevel(@Param("userProfileId") Long userProfileId);
}
