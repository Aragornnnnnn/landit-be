// AI 요청에 포함할 누적 대화 히스토리 메시지를 담는다.
package com.landit.landitbe.session.application.port;

public record AiConversationHistoryMessage(
        Long messageId,
        int turnNumber,
        String role,
        String content,
        String translatedContent
) {
}
