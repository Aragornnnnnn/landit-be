// 프리톡 대화 핵심 추출과 임베딩 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 대화 핵심 추출과 임베딩 요청을 담는다.
 *
 * @param sessionId 완료된 프리톡 세션 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param conversationHistory 완료된 세션의 누적 대화. 추출 대상은 사용자 발화이며 AI 발화는 짧은 응답 해석을 위한 맥락으로만 사용한다.
 */
public record AiConversationEmbeddingsRequest(
    Long sessionId,
    String targetLocale,
    String baseLocale,
    List<AiConversationHistoryMessage> conversationHistory) {}
