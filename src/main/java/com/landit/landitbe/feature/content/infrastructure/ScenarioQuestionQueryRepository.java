// 세션 진행에 필요한 시나리오 고정 질문을 조회한다.

package com.landit.landitbe.feature.content.infrastructure;

import com.landit.landitbe.feature.content.domain.ScenarioQuestion;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 세션 진행에 필요한 시나리오 고정 질문을 조회한다. */
@Repository
public interface ScenarioQuestionQueryRepository extends JpaRepository<ScenarioQuestion, Long> {

  /** 시나리오, 순서, 언어 조합에 맞는 활성 고정 질문을 조회한다. */
  @Query(
      """
            SELECT new com.landit.landitbe.feature.content.infrastructure.ScenarioQuestionRow(
                scenarioQuestion.id,
                scenarioQuestion.displayOrder,
                questionVariant.questionText,
                questionVariant.questionTranslation
            )
            FROM ScenarioQuestion scenarioQuestion
            JOIN ScenarioQuestionLanguageVariant questionVariant
              ON questionVariant.scenarioQuestionId = scenarioQuestion.id
            WHERE scenarioQuestion.scenarioId = :scenarioId
              AND scenarioQuestion.displayOrder = :displayOrder
              AND scenarioQuestion.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
              AND questionVariant.targetLocale = :targetLocale
              AND questionVariant.baseLocale = :baseLocale
              AND questionVariant.status = com.landit.landitbe.shared.domain.ActiveStatus.ACTIVE
      """)
  Optional<ScenarioQuestionRow> findActiveQuestion(
      @Param("scenarioId") long scenarioId,
      @Param("displayOrder") int displayOrder,
      @Param("targetLocale") Locale targetLocale,
      @Param("baseLocale") Locale baseLocale);
}
