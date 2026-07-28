// 프리톡 사용자 발화의 AI 응답 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 사용자 발화의 AI 응답 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param submittedMessageId 처리할 사용자 메시지 ID
 * @param submittedTurnNumber 처리할 사용자 메시지 턴 번호
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param partnerDisplayName 대화 상대 표시 이름
 * @param accentLocale 대화 상대 억양 locale
 * @param responseMode 응답 생성 방식
 * @param isFirstUserTurn 사용자 선시작의 첫 발화 여부
 * @param topic 선택했거나 추론된 주제
 * @param conversationHistory 누적 대화 메시지
 */
public record AiFreeTalkTurnRequest(
    Long sessionId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    String partnerDisplayName,
    String accentLocale,
    AiFreeTalkResponseMode responseMode,
    boolean isFirstUserTurn,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory) {}
