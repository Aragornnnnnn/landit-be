// 프리톡 맞춤 표현 추천 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 맞춤 표현 추천 결과를 담는다.
 *
 * @param recommendations 대화에 맞는 표현 추천 목록
 */
public record AiFreeTalkExpressionRecommendationsResult(
    List<AiFreeTalkExpressionRecommendation> recommendations) {}
