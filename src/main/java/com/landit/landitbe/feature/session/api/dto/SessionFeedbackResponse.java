// 세션 최종 피드백 API의 응답 구조를 정의한다.

package com.landit.landitbe.feature.session.api.dto;

import com.landit.landitbe.feature.session.application.port.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.feature.session.domain.FeedbackType;
import java.math.BigDecimal;
import java.util.List;

/** 세션 최종 피드백 API의 응답 구조를 정의한다. */
public record SessionFeedbackResponse(
    Long sessionId,
    Integer nativeScore,
    BigDecimal starRating,
    String highlightMessage,
    String summaryMessage,
    List<MessageFeedbackResponse> messageFeedbacks) {

  /** 내부 타입을 정의한다. */
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
      String benchmarkMessage) {}

  /** 내부 타입을 정의한다. */
  public record EvaluationContextResponse(
      AiMessageFeedbackEvaluationContextType type, String content, String translatedContent) {}
}
