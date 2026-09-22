// 사용자와 시나리오에 해당하는 완료 회차를 최신순으로 조회한다.

package com.landit.landitbe.feature.learning.scenario.history.repository;

import com.landit.landitbe.feature.learning.scenario.history.repository.projection.ScenarioHistoryProjection;
import com.landit.landitbe.feature.learning.scenario.session.domain.ScenarioSession;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** 사용자별 완료 시나리오 회차 조회를 담당한다. */
public interface ScenarioHistoryQueryRepository extends Repository<ScenarioSession, Long> {

  /**
   * 본인의 시나리오 완료 회차를 완료 시각과 세션 ID 내림차순으로 조회한다.
   *
   * @param userId 로그인 사용자 ID
   * @param scenarioId 조회할 시나리오 ID
   * @return 완료 회차 목록
   */
  @Query(
      """
      SELECT new com.landit.landitbe.feature.learning.scenario.history.repository.projection.ScenarioHistoryProjection(
          session.id, history.id, session.startedAt, session.endedAt,
          scenarioSession.userOpeningInstructionSnapshot)
      FROM ScenarioSession scenarioSession
      JOIN LearningSession session ON session.id = scenarioSession.learningSessionId
      JOIN ScenarioLanguageVariant variant ON variant.id = scenarioSession.scenarioLanguageVariantId
      JOIN SessionHistory history ON history.learningSessionId = session.id
      WHERE session.userProfileId = :userId AND history.userProfileId = :userId
        AND variant.scenarioId = :scenarioId
        AND session.sessionType = com.landit.landitbe.feature.learning.conversation.domain.SessionType.SCENARIO
        AND history.sessionType = com.landit.landitbe.feature.learning.conversation.domain.SessionType.SCENARIO
        AND session.status = com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus.COMPLETED
      ORDER BY session.endedAt DESC, session.id DESC
      """)
  List<ScenarioHistoryProjection> findCompleted(
      @Param("userId") long userId, @Param("scenarioId") long scenarioId);
}
