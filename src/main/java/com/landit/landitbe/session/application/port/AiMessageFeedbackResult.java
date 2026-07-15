// AI 메시지별 피드백 요청 접수 결과를 표현한다.
package com.landit.landitbe.session.application.port;

import com.landit.landitbe.session.domain.ProcessingStatus;

public record AiMessageFeedbackResult(
    Long sessionId, Long messageId, ProcessingStatus feedbackStatus) {}
