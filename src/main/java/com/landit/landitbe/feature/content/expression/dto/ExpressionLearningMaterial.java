// 표현 학습 검증과 응답 구성에 필요한 콘텐츠를 한 번의 조회 값으로 전달한다.

package com.landit.landitbe.feature.content.expression.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.content.expression.domain.WritingExpression;

/**
 * 사용자 학습 상태를 포함하지 않는 표현 콘텐츠 값이다.
 *
 * @param content 학습 연결과 난이도
 * @param detail 표현 본문과 대표 예문
 * @param practiceExamplesPayload 추가 연습 예문
 */
public record ExpressionLearningMaterial(
    ExpressionLearningContent content,
    ExpressionLearningResponse detail,
    JsonNode practiceExamplesPayload) {
  /**
   * 영속 표현의 콘텐츠를 값으로 복사한다.
   *
   * @param expression 원본 표현
   * @return 학습 상태와 독립적인 콘텐츠 값
   */
  public static ExpressionLearningMaterial from(WritingExpression expression) {
    return new ExpressionLearningMaterial(
        ExpressionLearningContent.from(expression),
        ExpressionLearningResponse.from(expression, null, null, false),
        expression.getPracticeExamplesPayload() == null
            ? null
            : expression.getPracticeExamplesPayload().deepCopy());
  }
}
