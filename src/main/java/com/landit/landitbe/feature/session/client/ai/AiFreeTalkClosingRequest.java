// 프리톡 마지막 AI 메시지 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 마지막 AI 메시지 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param submittedMessageId 마지막 사용자 메시지 ID
 * @param submittedTurnNumber 마지막 사용자 메시지 턴 번호
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param closingReason 마무리 메시지 생성 사유
 * @param titleGenerationRequired 사용자 선시작 세션의 제목 생성 필요 여부
 * @param topic 저장된 프리톡 주제
 * @param conversationHistory 누적 대화 메시지
 */
public record AiFreeTalkClosingRequest(
    Long sessionId,
    String characterId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    AiFreeTalkClosingReason closingReason,
    boolean titleGenerationRequired,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory) {}
