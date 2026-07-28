// 프리톡 표현 추천에서 재사용할 기존 표현 후보를 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * 프리톡 표현 추천에서 재사용할 기존 표현 후보를 담는다.
 *
 * @param expressionId 기존 공통 표현 ID
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 뜻
 * @param usageSummary 짧은 용법 설명
 */
public record AiFreeTalkExistingExpression(
    Long expressionId,
    String targetExpressionText,
    String baseExpressionMeaningText,
    String usageSummary) {}
