// 프리톡 표현 추천에 사용할 기존 표현 후보를 전달한다.

package com.landit.landitbe.feature.content.dto;

/** 프리톡 표현 추천에 사용할 기존 표현 후보를 전달한다. */
public record ExpressionRecommendationCandidate(
    Long expressionId,
    String targetExpressionText,
    String baseExpressionMeaningText,
    String usageSummary) {}
