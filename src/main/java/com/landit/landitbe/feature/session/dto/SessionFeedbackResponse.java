// 세션 최종 피드백 API의 응답 구조를 정의한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.client.ai.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.feature.session.domain.FeedbackType;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessageFeedback;
import com.landit.landitbe.feature.session.domain.SessionHistorySummaryFeedback;
import java.math.BigDecimal;
import java.util.List;

/**
 * 세션 최종 피드백 API의 응답 구조를 정의한다.
 *
 * @param sessionId 학습 세션 ID
 * @param nativeScore 원어민 관점 점수
 * @param starRating 세션 별점
 * @param highlightMessage 최종 피드백 강조 메시지
 * @param summaryMessage 최종 피드백 요약
 * @param messageFeedbacks 메시지별 피드백 목록
 */
public record SessionFeedbackResponse(
    Long sessionId,
    Integer nativeScore,
    BigDecimal starRating,
    String highlightMessage,
    String summaryMessage,
    List<MessageFeedbackResponse> messageFeedbacks) {

  /**
   * 저장된 세션 요약 피드백과 메시지별 응답을 최종 피드백 응답으로 변환한다.
   *
   * @param sessionId 학습 세션 ID
   * @param summary 저장된 세션 요약 피드백
   * @param messageFeedbacks 메시지별 피드백 응답
   * @return 세션 최종 피드백 응답
   */
  public static SessionFeedbackResponse from(
      Long sessionId,
      SessionHistorySummaryFeedback summary,
      List<MessageFeedbackResponse> messageFeedbacks) {
    return new SessionFeedbackResponse(
        sessionId,
        summary.getNativeScore(),
        summary.getStarRating(),
        summary.getHighlightMessage(),
        summary.getSummaryMessage(),
        messageFeedbacks);
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param messageFeedbackId 메시지 피드백 ID
   * @param messageId 메시지 ID
   * @param turnNumber 대화 턴 번호
   * @param userMessage 사용자 메시지
   * @param evaluationContext 메시지 평가 기준
   * @param feedbackType 피드백 유형
   * @param baseLocaleAnalogy 기준 언어 비유 설명
   * @param positiveFeedback 긍정 피드백
   * @param feedbackDetail 상세 피드백
   * @param correctionExpression 교정 표현
   * @param correctionReason 교정 사유
   * @param benchmarkMessage 비교용 모범 메시지
   */
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
      String benchmarkMessage) {

    /**
     * 저장된 메시지 피드백과 평가 당시 메시지 정보를 응답으로 변환한다.
     *
     * @param feedback 저장된 메시지 피드백
     * @param turnNumber 대화 턴 번호
     * @param userMessage 사용자 메시지
     * @param evaluationContext 메시지 평가 기준
     * @return 메시지별 피드백 응답
     */
    public static MessageFeedbackResponse from(
        SessionHistoryMessageFeedback feedback,
        int turnNumber,
        String userMessage,
        EvaluationContextResponse evaluationContext) {
      return new MessageFeedbackResponse(
          feedback.getId(),
          feedback.getSessionHistoryMessageId(),
          turnNumber,
          userMessage,
          evaluationContext,
          feedback.getFeedbackType(),
          feedback.getBaseLocaleAnalogy(),
          feedback.getPositiveFeedback(),
          feedback.getFeedbackDetail(),
          feedback.getCorrectionExpression(),
          feedback.getCorrectionReason(),
          feedback.getBenchmarkMessage());
    }
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param type 평가 컨텍스트 유형
   * @param content 메시지 본문
   * @param translatedContent 번역된 메시지 본문
   */
  public record EvaluationContextResponse(
      AiMessageFeedbackEvaluationContextType type, String content, String translatedContent) {

    /**
     * 평가 컨텍스트 값을 API 응답으로 변환한다.
     *
     * @param type 평가 컨텍스트 유형
     * @param content 메시지 본문
     * @param translatedContent 번역된 메시지 본문
     * @return 메시지 평가 컨텍스트 응답
     */
    public static EvaluationContextResponse from(
        AiMessageFeedbackEvaluationContextType type, String content, String translatedContent) {
      return new EvaluationContextResponse(type, content, translatedContent);
    }
  }
}
