// 관리자 시나리오 테스트 목록을 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.Scenario;
import com.landit.landitbe.feature.content.repository.projection.ScenarioListProjection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** 관리자 시나리오 테스트 목록 조회를 소유한다. */
public interface AdminScenarioListQueryRepository extends Repository<Scenario, Long> {

  /**
   * 관리자 테스트에 사용할 활성 콘텐츠만 조회한다.
   *
   * @param userId 관리자 사용자 ID
   * @return 사용자 언어 설정에 맞는 활성 시나리오 목록
   */
  @Query(
      """
            SELECT new com.landit.landitbe.feature.content.repository.projection.ScenarioListProjection(
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
                usp.bestStarRating
            )
            FROM UserProfile up
            JOIN CategoryLanguageVariant clv
              ON clv.baseLocale = up.baseLocale
            JOIN Category c
              ON c.id = clv.categoryId
             AND c.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            JOIN Scenario s
              ON s.categoryId = c.id
             AND s.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            JOIN ScenarioLanguageVariant slv
              ON slv.scenarioId = s.id
             AND slv.targetLocale = up.targetLocale
             AND slv.baseLocale = up.baseLocale
             AND slv.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN ScenarioQuestion openingQuestion
              ON openingQuestion.scenarioId = s.id
             AND openingQuestion.displayOrder = 1
             AND openingQuestion.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN ScenarioQuestionLanguageVariant openingQuestionVariant
              ON openingQuestionVariant.scenarioQuestionId = openingQuestion.id
             AND openingQuestionVariant.targetLocale = up.targetLocale
             AND openingQuestionVariant.baseLocale = up.baseLocale
             AND openingQuestionVariant.status =
                 com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN ConversationCharacter character
              ON character.characterId = s.characterId
             AND character.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN TtsVoice tv
              ON tv.id = character.ttsVoiceId
             AND tv.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN UserScenarioProgress usp
              ON usp.userProfileId = up.id
             AND usp.scenarioId = s.id
             AND usp.targetLocale = up.targetLocale
            WHERE up.id = :userId
            ORDER BY c.displayOrder ASC, s.displayOrder ASC, s.id ASC
      """)
  List<ScenarioListProjection> findActiveScenarioList(@Param("userId") long userId);
}
