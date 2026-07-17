// 시나리오 세션 시작에 필요한 콘텐츠와 잠금 정보를 조회한다.

package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.content.domain.Scenario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 시나리오 세션 시작에 필요한 콘텐츠와 잠금 정보를 조회한다. */
public interface ScenarioSessionStartQueryRepository extends JpaRepository<Scenario, Long> {

  /** 사용자 언어 설정에 맞는 시나리오 시작 정보를 조회한다. */
  @Query(
      """
            SELECT new com.landit.landitbe.session.infrastructure.ScenarioSessionStartRow(
                s.id,
                s.categoryId,
                c.status,
                s.status,
                slv.id,
                slv.status,
                s.firstSpeaker,
                s.totalQuestionCount,
                slv.userOpeningInstruction,
                openingQuestionVariant.questionText,
                openingQuestionVariant.questionTranslation,
                openingQuestionVariant.innerThought,
                openingQuestionVariant.innerThoughtType,
                tv.provider,
                tv.model,
                tv.providerVoiceId,
                tv.gender
            )
            FROM UserProfile up
            JOIN Scenario s
              ON s.id = :scenarioId
            JOIN Category c
              ON c.id = s.categoryId
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
            WHERE up.id = :userId
      """)
  Optional<ScenarioSessionStartRow> findStartRow(
      @Param("userId") long userId, @Param("scenarioId") long scenarioId);

  /** 시작할 시나리오의 직전 displayOrder 시나리오 완료 상태를 조회한다. */
  @Query(
      """
            SELECT new com.landit.landitbe.session.infrastructure.ScenarioSessionLockRow(
                previousScenario.id,
                usp.status
            )
            FROM UserProfile up
            JOIN Scenario currentScenario
              ON currentScenario.id = :scenarioId
            JOIN Scenario previousScenario
              ON previousScenario.categoryId = currentScenario.categoryId
             AND previousScenario.displayOrder = (
                SELECT MAX(candidate.displayOrder)
                FROM Scenario candidate
                JOIN ScenarioLanguageVariant candidateVariant
                  ON candidateVariant.scenarioId = candidate.id
                 AND candidateVariant.targetLocale = up.targetLocale
                 AND candidateVariant.baseLocale = up.baseLocale
                WHERE candidate.categoryId = currentScenario.categoryId
                  AND candidate.displayOrder < currentScenario.displayOrder
             )
            LEFT JOIN UserScenarioProgress usp
              ON usp.userProfileId = up.id
             AND usp.scenarioId = previousScenario.id
             AND usp.targetLocale = up.targetLocale
            WHERE up.id = :userId
      """)
  Optional<ScenarioSessionLockRow> findPreviousScenarioLockRow(
      @Param("userId") long userId, @Param("scenarioId") long scenarioId);
}
