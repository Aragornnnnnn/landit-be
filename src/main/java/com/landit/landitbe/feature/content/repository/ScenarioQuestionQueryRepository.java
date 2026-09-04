// 세션 진행에 필요한 시나리오 고정 질문을 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.domain.ScenarioQuestion;
import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 세션 진행에 필요한 시나리오 고정 질문을 조회한다. */
@Repository
public interface ScenarioQuestionQueryRepository extends JpaRepository<ScenarioQuestion, Long> {

  /**
   * 시나리오, 순서, 질문 레벨 그룹, 언어 조합이 정확히 일치하는 활성 고정 질문을 조회한다.
   *
   * @param scenarioId 질문이 속한 시나리오 ID
   * @param displayOrder 시나리오 안에서의 질문 순서
   * @param questionLevelGroup 사용자 학습 레벨에 대응하는 질문 레벨 그룹
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @return 모든 조회 조건과 정확히 일치하는 활성 고정 질문. 없으면 빈 값
   */
  @Query(
      """
            SELECT new com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection(
                scenarioQuestion.id,
                scenarioQuestion.displayOrder,
                questionVariant.questionText,
                questionVariant.questionTranslation,
                questionVariant.audioUrl,
                scenarioQuestion.responseDemand,
                questionVariant.requiredResponseElement
            )
            FROM ScenarioQuestion scenarioQuestion
            JOIN ScenarioQuestionLanguageVariant questionVariant
              ON questionVariant.scenarioQuestionId = scenarioQuestion.id
            WHERE scenarioQuestion.scenarioId = :scenarioId
              AND scenarioQuestion.displayOrder = :displayOrder
              AND scenarioQuestion.questionLevelGroup = :questionLevelGroup
              AND scenarioQuestion.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
              AND questionVariant.targetLocale = :targetLocale
              AND questionVariant.baseLocale = :baseLocale
              AND questionVariant.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
      """)
  Optional<ScenarioQuestionProjection> findActiveQuestion(
      @Param("scenarioId") long scenarioId,
      @Param("displayOrder") int displayOrder,
      @Param("questionLevelGroup") ContentLearningLevel questionLevelGroup,
      @Param("targetLocale") Locale targetLocale,
      @Param("baseLocale") Locale baseLocale);
}
