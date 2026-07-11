// AI 메시지별 피드백 요청에 필요한 직전 AI 메시지와 사용자 메시지를 담는다.
package com.landit.landitbe.session.application.port;

public record AiMessageFeedbackContext(
        String aiMessage,
        String aiMessageTranslation,
        String userMessage
) {
}
