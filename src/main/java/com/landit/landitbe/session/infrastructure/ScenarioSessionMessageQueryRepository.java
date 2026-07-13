// 사용자 발화 제출에 필요한 시나리오 컨텍스트를 조회한다.
package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.session.domain.ScenarioSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioSessionMessageQueryRepository extends JpaRepository<ScenarioSession, Long> {

    /** 학습 세션 ID로 AI 요청 구성에 필요한 시나리오 정보를 조회한다. */
    @Query("""
            SELECT new com.landit.landitbe.session.infrastructure.ScenarioSessionMessageContextRow(
                scenario.id,
                scenarioVariant.title,
                scenarioVariant.briefing,
                scenarioVariant.conversationGoal,
                scenario.aiRole,
                scenario.firstSpeaker,
                scenarioVariant.userOpeningInstruction,
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
    Optional<ScenarioSessionMessageContextRow> findContextByLearningSessionId(
            @Param("learningSessionId") long learningSessionId
    );
}
