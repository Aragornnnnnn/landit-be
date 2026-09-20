// 다른 기능에 표현의 본문과 의미를 불변 값으로 전달한다.

package com.landit.landitbe.feature.content.expression.dto;

import com.landit.landitbe.feature.content.expression.domain.WritingExpression;

/**
 * 표현 본문 조회 계약이다.
 *
 * @param id 표현 ID
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 의미
 */
public record ExpressionText(
    Long id, String targetExpressionText, String baseExpressionMeaningText) {
  /**
   * 표현의 본문을 복사한다.
   *
   * @param expression 원본 표현
   * @return 표현 본문
   */
  public static ExpressionText from(WritingExpression expression) {
    return new ExpressionText(
        expression.getId(),
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText());
  }
}
