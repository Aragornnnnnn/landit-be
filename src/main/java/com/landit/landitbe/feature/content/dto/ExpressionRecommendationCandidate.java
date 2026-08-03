// 프리톡 표현 추천에 사용할 기존 표현 후보를 전달한다.

package com.landit.landitbe.feature.content.dto;

/**
 * 프리톡 표현 추천에 사용할 기존 표현 후보를 전달한다.
 *
 * @param expressionId 표현 ID
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 뜻
 * @param usageSummary 짧은 용법 설명
 */
public record ExpressionRecommendationCandidate(
    Long expressionId,
    String targetExpressionText,
    String baseExpressionMeaningText,
    String usageSummary) {}
