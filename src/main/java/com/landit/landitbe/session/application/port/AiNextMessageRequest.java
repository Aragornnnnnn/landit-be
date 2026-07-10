// AI 다음 메시지 생성 요청 본문을 표현한다.
package com.landit.landitbe.session.application.port;

import java.util.List;

public record AiNextMessageRequest(
        Long sessionId,
        Long submittedMessageId,
        int submittedTurnNumber,
        AiScenarioContext scenario,
        List<AiConversationHistoryMessage> conversationHistory,
        AiNextQuestion nextQuestion
) {
}
