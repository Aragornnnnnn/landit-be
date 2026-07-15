// AI 메시지별 피드백에서 사용자 발화를 평가할 기준 정보를 담는다.
package com.landit.landitbe.session.application.port;

public record AiMessageFeedbackEvaluationContext(
    AiMessageFeedbackEvaluationContextType type, String content, String translatedContent) {}
