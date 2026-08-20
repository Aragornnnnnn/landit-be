// 프리톡 대화에서 학습할 표현 추천 한 건을 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * 프리톡 대화에서 학습할 표현 추천 한 건을 담는다.
 *
 * @param displayOrder 노출 순서
 * @param existingExpressionId 재사용할 기존 표현 ID
 */
public record AiFreeTalkExpressionRecommendation(int displayOrder, Long existingExpressionId) {}
