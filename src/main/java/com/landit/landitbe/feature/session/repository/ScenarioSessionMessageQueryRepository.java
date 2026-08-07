// 사용자 발화 제출에 필요한 시나리오 컨텍스트를 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.ScenarioSession;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자 발화 제출에 필요한 시나리오 컨텍스트를 조회한다. */
public interface ScenarioSessionMessageQueryRepository
    extends JpaRepository<ScenarioSession, Long> {

  /** 학습 세션 ID로 AI 요청 구성에 필요한 시나리오 정보를 조회한다. */
  @Query(
      """
            SELECT new com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection(
                scenario.id,
                scenarioVariant.title,
                scenarioVariant.briefing,
                scenarioVariant.conversationGoal,
                scenario.aiRole,
                scenario.firstSpeaker,
                scenarioSession.userOpeningInstructionSnapshot,
                scenario.totalQuestionCount,
                scenarioVariant.targetLocale,
                scenarioVariant.baseLocale
            )
            FROM ScenarioSession scenarioSession
            JOIN ScenarioLanguageVariant scenarioVariant
              ON scenarioVariant.id = scenarioSession.scenarioLanguageVariantId
            JOIN Scenario scenario
              ON scenario.id = scenarioVariant.scenarioId
            WHERE scenarioSession.learningSessionId = :learningSessionId
      """)
  Optional<ScenarioSessionMessageContextProjection> findContextByLearningSessionId(
      @Param("learningSessionId") long learningSessionId);
}
