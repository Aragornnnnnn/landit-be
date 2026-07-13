// 세션 최종 피드백 API의 응답 구조를 정의한다.
package com.landit.landitbe.session.api.dto;

import com.landit.landitbe.session.application.port.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.session.domain.FeedbackType;
import java.math.BigDecimal;
import java.util.List;

public record SessionFeedbackResponse(
        Long sessionId,
        Integer nativeScore,
        BigDecimal starRating,
        String highlightMessage,
        String summaryMessage,
        List<MessageFeedbackResponse> messageFeedbacks
) {

    public record MessageFeedbackResponse(
            Long messageFeedbackId,
            Long messageId,
            int turnNumber,
            String userMessage,
            EvaluationContextResponse evaluationContext,
            FeedbackType feedbackType,
            String baseLocaleAnalogy,
            String positiveFeedback,
            String feedbackDetail,
            String correctionExpression,
            String correctionReason,
            String benchmarkMessage
    ) {
    }

    public record EvaluationContextResponse(
            AiMessageFeedbackEvaluationContextType type,
            String content,
            String translatedContent
    ) {
    }
}
