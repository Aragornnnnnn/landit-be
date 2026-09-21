// 표현 재사용 판정에 후보로 보내는, 사용자가 이전에 배운 표현 하나를 담는다.

package com.landit.landitbe.feature.learning.freetalk.expression.client.ai;

/**
 * 표현 재사용 판정에 후보로 보내는, 사용자가 이전에 배운 표현이다.
 *
 * @param expressionId 표현 ID. 예: 812
 * @param targetExpressionText 학습 언어 표현. 예: "grab a coffee"
 * @param baseExpressionMeaningText 기준 언어 뜻. 예: "커피 한잔하다"
 */
public record AiFreeTalkLearnedExpression(
    Long expressionId, String targetExpressionText, String baseExpressionMeaningText) {}
