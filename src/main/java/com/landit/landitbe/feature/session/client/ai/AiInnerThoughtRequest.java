// AI 속마음 생성을 요청하는 대화 컨텍스트를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * AI 속마음 생성을 요청하는 대화 컨텍스트를 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param submittedMessageId 제출한 사용자 메시지 ID
 * @param submittedTurnNumber 제출한 사용자 메시지의 턴 번호
 * @param scenario AI 요청용 시나리오 컨텍스트
 * @param conversationHistory 이전 대화 메시지 목록
 */
public record AiInnerThoughtRequest(
    Long sessionId,
    Long submittedMessageId,
    int submittedTurnNumber,
    AiScenarioContext scenario,
    List<AiConversationHistoryMessage> conversationHistory) {}
