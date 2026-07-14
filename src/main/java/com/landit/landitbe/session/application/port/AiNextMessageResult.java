// AI 다음 메시지 생성 결과를 표현한다.
package com.landit.landitbe.session.application.port;

import com.landit.landitbe.session.domain.GoalCompletionStatus;

public record AiNextMessageResult(
        String aiMessage,
        String translatedMessage,
        GoalCompletionStatus goalCompletionStatus
) {
}
