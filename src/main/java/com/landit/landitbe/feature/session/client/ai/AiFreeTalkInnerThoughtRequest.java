// 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 학습 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param submittedMessageId 속마음 대상 사용자 메시지 ID
 * @param submittedTurnNumber 속마음 대상 사용자 메시지 턴 번호
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 사용자 기준 언어
 * @param topic 현재 프리톡 주제
 * @param conversationHistory 속마음 생성에 사용할 대화 문맥
 */
public record AiFreeTalkInnerThoughtRequest(
    Long sessionId,
    String characterId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory) {}
