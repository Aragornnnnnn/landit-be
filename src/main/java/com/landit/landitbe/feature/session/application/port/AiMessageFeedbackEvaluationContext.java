// AI 메시지별 피드백에서 사용자 발화를 평가할 기준 정보를 담는다.

package com.landit.landitbe.feature.session.application.port;

/** AI 메시지별 피드백에서 사용자 발화를 평가할 기준 정보를 담는다. */
public record AiMessageFeedbackEvaluationContext(
    AiMessageFeedbackEvaluationContextType type, String content, String translatedContent) {}
