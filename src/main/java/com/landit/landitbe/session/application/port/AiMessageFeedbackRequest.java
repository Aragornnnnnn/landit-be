// AI 메시지별 피드백 생성을 요청하는 본문을 표현한다.
package com.landit.landitbe.session.application.port;

public record AiMessageFeedbackRequest(
        Long sessionId,
        Long messageId,
        int turnNumber,
        int messageSequence,
        AiScenarioContext scenario,
        AiMessageFeedbackContext messageContext
) {
}
