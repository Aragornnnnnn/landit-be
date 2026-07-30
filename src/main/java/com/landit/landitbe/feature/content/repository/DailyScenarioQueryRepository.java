// 날짜별 시나리오 조회에 필요한 단건 콘텐츠를 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.Scenario;
import com.landit.landitbe.feature.content.repository.projection.DailyScenarioProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 날짜별 시나리오 조회에 필요한 단건 콘텐츠를 조회한다. */
@Repository
public interface DailyScenarioQueryRepository extends JpaRepository<Scenario, Long> {

  /**
   * 사용자 언어 설정에 맞는 시나리오 단건 정보를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 사용자 언어 설정에 맞는 시나리오 단건 정보
   */
  @Query(
      """
            SELECT new com.landit.landitbe.feature.content.repository.projection.DailyScenarioProjection(
                s.id,
                slv.title,
                slv.briefing,
                slv.conversationGoal,
                s.thumbnailUrl,
                s.difficulty,
                s.firstSpeaker,
                openingQuestionVariant.questionText,
                openingQuestionVariant.questionTranslation,
                slv.userOpeningInstruction,
                openingQuestionVariant.innerThought,
                openingQuestionVariant.innerThoughtType,
                tv.provider,
                tv.model,
                tv.providerVoiceId,
                tv.gender,
                usp.bestStarRating
            )
            FROM UserProfile up
            JOIN Scenario s
              ON s.id = :scenarioId
            JOIN ScenarioLanguageVariant slv
              ON slv.scenarioId = s.id
             AND slv.targetLocale = up.targetLocale
             AND slv.baseLocale = up.baseLocale
            LEFT JOIN ScenarioQuestion openingQuestion
              ON openingQuestion.scenarioId = s.id
             AND openingQuestion.displayOrder = 1
             AND openingQuestion.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN ScenarioQuestionLanguageVariant openingQuestionVariant
              ON openingQuestionVariant.scenarioQuestionId = openingQuestion.id
             AND openingQuestionVariant.targetLocale = up.targetLocale
             AND openingQuestionVariant.baseLocale = up.baseLocale
             AND openingQuestionVariant.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN TtsVoice tv
              ON tv.id = slv.ttsVoiceId
             AND tv.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN UserScenarioProgress usp
              ON usp.userProfileId = up.id
             AND usp.scenarioId = s.id
             AND usp.targetLocale = up.targetLocale
            WHERE up.id = :userId
      """)
  Optional<DailyScenarioProjection> findDailyScenario(
      @Param("userId") long userId, @Param("scenarioId") long scenarioId);
}
