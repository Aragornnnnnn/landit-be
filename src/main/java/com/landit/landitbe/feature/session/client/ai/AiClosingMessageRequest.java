// AI 종료 메시지 생성 요청 본문을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.GoalCompletionStatus;
import java.util.List;

/**
 * AI 종료 메시지 생성 요청 본문을 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param submittedMessageId 제출한 사용자 메시지 ID
 * @param submittedTurnNumber 제출한 사용자 메시지의 턴 번호
 * @param scenario AI 요청용 시나리오 컨텍스트
 * @param conversationHistory 이전 대화 메시지 목록
 * @param closingReason 대화 종료 사유
 * @param goalCompletionStatus 대화 목표 달성 상태
 */
public record AiClosingMessageRequest(
    Long sessionId,
    Long submittedMessageId,
    int submittedTurnNumber,
    AiScenarioContext scenario,
    List<AiConversationHistoryMessage> conversationHistory,
    AiClosingReason closingReason,
    GoalCompletionStatus goalCompletionStatus) {}
