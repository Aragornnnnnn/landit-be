// AI 종료 메시지 생성 요청 본문을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.GoalCompletionStatus;
import java.util.List;

/** AI 종료 메시지 생성 요청 본문을 표현한다. */
public record AiClosingMessageRequest(
    Long sessionId,
    Long submittedMessageId,
    int submittedTurnNumber,
    AiScenarioContext scenario,
    List<AiConversationHistoryMessage> conversationHistory,
    AiClosingReason closingReason,
    GoalCompletionStatus goalCompletionStatus) {}
