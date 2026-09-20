// 표현 학습 검증에 필요한 콘텐츠 정보를 전달한다.

package com.landit.landitbe.feature.content.expression.dto;

import com.landit.landitbe.feature.content.expression.domain.WritingExpression;
import com.landit.landitbe.feature.content.expression.domain.WritingExpressionSource;

/**
 * 표현의 학습 연결과 난이도다.
 *
 * @param scenarioId 연결된 시나리오 ID
 * @param expressionSource 표현 출처
 * @param difficultyLevel 난이도
 */
public record ExpressionLearningContent(
    Long scenarioId, WritingExpressionSource expressionSource, int difficultyLevel) {
  /**
   * 표현의 학습 정보를 복사한다.
   *
   * @param expression 원본 표현
   * @return 학습 정보
   */
  public static ExpressionLearningContent from(WritingExpression expression) {
    return new ExpressionLearningContent(
        expression.getScenarioId(),
        expression.getExpressionSource(),
        expression.getDifficultyLevel());
  }
}
