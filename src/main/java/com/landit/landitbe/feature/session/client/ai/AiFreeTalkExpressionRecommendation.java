// 프리톡 대화에서 학습할 표현 추천 한 건을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;

/**
 * 프리톡 대화에서 학습할 표현 추천 한 건을 담는다.
 *
 * @param displayOrder 노출 순서
 * @param sourceType 기존 표현 재사용 여부
 * @param existingExpressionId 재사용할 기존 표현 ID
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 뜻
 * @param usageSummary 짧은 용법 설명
 */
public record AiFreeTalkExpressionRecommendation(
    int displayOrder,
    FreeTalkExpressionSourceType sourceType,
    Long existingExpressionId,
    String targetExpressionText,
    String baseExpressionMeaningText,
    String usageSummary) {}
