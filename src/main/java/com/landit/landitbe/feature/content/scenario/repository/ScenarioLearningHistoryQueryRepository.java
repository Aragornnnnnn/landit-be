// 복습 콘텐츠 선택에 필요한 최초 완료 수준을 공유 DB에서 조회한다.

package com.landit.landitbe.feature.content.scenario.repository;

import com.landit.landitbe.feature.content.scenario.domain.Scenario;
import com.landit.landitbe.feature.content.scenario.repository.projection.CompletedScenarioLevelProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** 세션 상태를 변경하지 않는 복습 콘텐츠 조회 경계다. */
public interface ScenarioLearningHistoryQueryRepository extends Repository<Scenario, Long> {
  /**
   * 사용자와 언어별 최초 완료 세션의 수준을 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 대상 언어
   * @return 최초 완료 당시 수준. 이력이 없으면 빈 값
   */
  @Query(
      value =
          """
          SELECT ss.question_level_group AS questionLevelGroup,
                 COALESCE(a.current_level, ss.learning_level_at_completion) AS currentLevel,
                 ls.ended_at AS endedAt
          FROM scenario_session ss
          JOIN learning_session ls ON ls.id = ss.learning_session_id
          JOIN scenario_language_variant v ON v.id = ss.scenario_language_variant_id
          LEFT JOIN user_level_assessment a ON a.learning_session_id = ls.id
          WHERE ls.user_profile_id = :userId AND v.scenario_id = :scenarioId
            AND ls.target_locale = :targetLocale AND ls.status = 'COMPLETED'
          ORDER BY ls.ended_at ASC, ls.id ASC
          LIMIT 1
          """,
      nativeQuery = true)
  Optional<CompletedScenarioLevelProjection> findFirstCompletedLevel(
      @Param("userId") long userId,
      @Param("scenarioId") long scenarioId,
      @Param("targetLocale") String targetLocale);
}
