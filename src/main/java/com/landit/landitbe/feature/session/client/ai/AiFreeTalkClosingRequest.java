// 프리톡 마지막 AI 메시지 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 마지막 AI 메시지 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param submittedMessageId 마지막 사용자 메시지 ID
 * @param submittedTurnNumber 마지막 사용자 메시지 턴 번호
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param closingReason 마무리 메시지 생성 사유
 * @param topic 저장된 프리톡 주제
 * @param conversationHistory 누적 대화 메시지
 */
public record AiFreeTalkClosingRequest(
    Long sessionId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    AiFreeTalkClosingReason closingReason,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory) {}
