// 최초 완료한 학습 이력에서 복습 콘텐츠의 수준을 조회한다.

package com.landit.landitbe.feature.learning.scenario.level.repository;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.learning.scenario.level.dto.CompletedScenarioLevel;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 공유 DB에서 최초 완료 당시 수준을 읽는 학습 전용 조회 저장소다. */
@Repository
@RequiredArgsConstructor
public class ScenarioLearningHistoryQueryRepository {
  private static final String FIRST_COMPLETED_LEVEL_SQL =
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
      """;

  private final EntityManager entityManager;

  /**
   * 사용자와 언어별 최초 완료 시나리오의 수준을 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 대상 언어
   * @return 최초 완료 당시 수준. 이력이 없으면 빈 값
   */
  public Optional<CompletedScenarioLevel> findFirstCompletedLevel(
      long userId, long scenarioId, String targetLocale) {
    var rows =
        entityManager
            .createNativeQuery(FIRST_COMPLETED_LEVEL_SQL)
            .setParameter("userId", userId)
            .setParameter("scenarioId", scenarioId)
            .setParameter("targetLocale", targetLocale)
            .getResultList();
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    Object[] row = (Object[]) rows.getFirst();
    LocalDateTime endedAt =
        row[2] instanceof LocalDateTime time ? time : ((Timestamp) row[2]).toLocalDateTime();
    return Optional.of(
        new CompletedScenarioLevel(
            ContentLearningLevel.valueOf((String) row[0]),
            row[1] == null ? null : ((Number) row[1]).intValue(),
            endedAt));
  }
}
