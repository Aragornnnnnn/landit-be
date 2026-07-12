// 시나리오 목록 화면에 필요한 읽기 전용 JPA 조회를 정의한다.
package com.landit.landitbe.content.infrastructure;

import com.landit.landitbe.content.domain.Scenario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScenarioListQueryRepository extends JpaRepository<Scenario, Long> {

    /** 사용자 기본 언어 조합에 해당하는 목록 데이터를 조회한다. Entity 연관관계가 없는 FK는 명시적으로 join한다. */
    @Query("""
            SELECT new com.landit.landitbe.content.infrastructure.ScenarioListRow(
                c.id,
                clv.name,
                c.displayOrder,
                c.status,
                s.id,
                s.displayOrder,
                slv.title,
                slv.briefing,
                slv.conversationGoal,
                s.difficulty,
                s.firstSpeaker,
                s.thumbnailUrl,
                s.status,
                slv.status,
                openingQuestionVariant.questionText,
                openingQuestionVariant.questionTranslation,
                slv.userOpeningInstruction,
                openingQuestionVariant.innerThought,
                openingQuestionVariant.innerThoughtType,
                tv.provider,
                tv.model,
                tv.providerVoiceId,
                tv.gender,
                usp.status,
                usp.bestStarRating
            )
            FROM UserProfile up
            JOIN CategoryLanguageVariant clv
              ON clv.baseLocale = up.baseLocale
            JOIN Category c
              ON c.id = clv.categoryId
            JOIN Scenario s
              ON s.categoryId = c.id
            JOIN ScenarioLanguageVariant slv
              ON slv.scenarioId = s.id
             AND slv.targetLocale = up.targetLocale
             AND slv.baseLocale = up.baseLocale
            LEFT JOIN ScenarioQuestion openingQuestion
              ON openingQuestion.scenarioId = s.id
             AND openingQuestion.displayOrder = 1
             AND openingQuestion.status = com.landit.landitbe.common.domain.ActiveStatus.ACTIVE
            LEFT JOIN ScenarioQuestionLanguageVariant openingQuestionVariant
              ON openingQuestionVariant.scenarioQuestionId = openingQuestion.id
             AND openingQuestionVariant.targetLocale = up.targetLocale
             AND openingQuestionVariant.baseLocale = up.baseLocale
             AND openingQuestionVariant.status = com.landit.landitbe.common.domain.ActiveStatus.ACTIVE
            LEFT JOIN TtsVoice tv
              ON tv.id = slv.ttsVoiceId
             AND tv.status = com.landit.landitbe.common.domain.ActiveStatus.ACTIVE
            LEFT JOIN UserScenarioProgress usp
              ON usp.userProfileId = up.id
             AND usp.scenarioId = s.id
             AND usp.targetLocale = up.targetLocale
            WHERE up.id = :userId
            ORDER BY c.displayOrder ASC, s.displayOrder ASC
            """)
    List<ScenarioListRow> findScenarioList(@Param("userId") long userId);
}
