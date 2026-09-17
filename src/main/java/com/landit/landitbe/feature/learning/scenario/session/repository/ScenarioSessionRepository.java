// 시나리오 세션 보조 엔티티의 저장을 담당한다.

package com.landit.landitbe.feature.learning.scenario.session.repository;

import com.landit.landitbe.feature.learning.scenario.session.domain.ScenarioSession;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 시나리오 세션 보조 엔티티의 저장을 담당한다. */
public interface ScenarioSessionRepository extends JpaRepository<ScenarioSession, Long> {

  /** 학습 세션 ID로 시나리오 세션 보조 정보를 조회한다. */
  Optional<ScenarioSession> findByLearningSessionId(Long learningSessionId);

  /**
   * 사용자가 특정 시각 이후 시작한 시나리오 세션 가운데 처음 완료한 세션의 ID를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param since 이 시각 이상에 시작한 세션만 센다
   * @return 완료 시각과 ID 오름차순의 첫 세션 ID. 없으면 빈 값
   */
  @Query(
      value =
          """
          SELECT ls.id
          FROM scenario_session ss
          JOIN learning_session ls ON ls.id = ss.learning_session_id
          JOIN scenario_language_variant v ON v.id = ss.scenario_language_variant_id
          WHERE ls.user_profile_id = :userId AND v.scenario_id = :scenarioId
            AND ls.status = 'COMPLETED' AND ls.started_at >= :since
          ORDER BY ls.ended_at ASC, ls.id ASC
          LIMIT 1
          """,
      nativeQuery = true)
  Optional<Long> findFirstCompletedSessionIdSince(
      @Param("userId") long userId,
      @Param("scenarioId") long scenarioId,
      @Param("since") LocalDateTime since);
}
