// 시나리오 세션 시작에 필요한 콘텐츠와 잠금 정보를 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.content.domain.Scenario;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionStartProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 시나리오 세션 시작에 필요한 콘텐츠와 잠금 정보를 조회한다. */
public interface ScenarioSessionStartQueryRepository extends JpaRepository<Scenario, Long> {

  /**
   * 사용자 언어 설정에 맞는 시나리오 시작 정보를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 사용자 언어 설정에 맞는 시나리오 시작 정보
   */
  @Query(
      """
            SELECT new com.landit.landitbe.feature.session.repository.projection.ScenarioSessionStartProjection(
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
             AND openingQuestion.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN ScenarioQuestionLanguageVariant openingQuestionVariant
              ON openingQuestionVariant.scenarioQuestionId = openingQuestion.id
             AND openingQuestionVariant.targetLocale = up.targetLocale
             AND openingQuestionVariant.baseLocale = up.baseLocale
             AND openingQuestionVariant.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            LEFT JOIN TtsVoice tv
              ON tv.id = slv.ttsVoiceId
             AND tv.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
            WHERE up.id = :userId
      """)
  Optional<ScenarioSessionStartProjection> findStartRow(
      @Param("userId") long userId, @Param("scenarioId") long scenarioId);
}
