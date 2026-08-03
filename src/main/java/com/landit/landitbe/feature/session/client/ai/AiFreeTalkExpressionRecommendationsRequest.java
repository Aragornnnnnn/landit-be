// 프리톡 맞춤 표현 추천 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 맞춤 표현 추천 요청을 담는다.
 *
 * @param sessionId 완료된 프리톡 세션 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param conversationHistory 완료된 세션의 누적 대화
 * @param existingExpressions 재사용 가능한 기존 표현 후보
 */
public record AiFreeTalkExpressionRecommendationsRequest(
    Long sessionId,
    String targetLocale,
    String baseLocale,
    List<AiConversationHistoryMessage> conversationHistory,
    List<AiFreeTalkExistingExpression> existingExpressions) {}
