// AI 세션 최종 피드백 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 세션 최종 피드백 생성 결과를 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param nativeScore 원어민 관점 점수
 * @param starRating 세션 별점
 * @param highlightMessage 최종 피드백 강조 메시지
 * @param summaryMessage 최종 피드백 요약
 * @param messageFeedbacks 메시지별 피드백 목록
 */
public record AiSessionFeedbackResult(
    Long sessionId,
    int nativeScore,
    BigDecimal starRating,
    String highlightMessage,
    String summaryMessage,
    List<AiSessionMessageFeedbackResult> messageFeedbacks,
    AiSessionLevelAssessment levelAssessment,
    boolean generationFallback) {

  /** 수준 평가가 없는 기존 클라이언트 결과를 만든다. */
  public AiSessionFeedbackResult(
      Long sessionId,
      int nativeScore,
      BigDecimal starRating,
      String highlightMessage,
      String summaryMessage,
      List<AiSessionMessageFeedbackResult> messageFeedbacks) {
    this(
        sessionId,
        nativeScore,
        starRating,
        highlightMessage,
        summaryMessage,
        messageFeedbacks,
        null,
        false);
  }

  /** 정상 생성된 수준 평가를 포함한 결과를 만든다. */
  public AiSessionFeedbackResult(
      Long sessionId,
      int nativeScore,
      BigDecimal starRating,
      String highlightMessage,
      String summaryMessage,
      List<AiSessionMessageFeedbackResult> messageFeedbacks,
      AiSessionLevelAssessment levelAssessment) {
    this(
        sessionId,
        nativeScore,
        starRating,
        highlightMessage,
        summaryMessage,
        messageFeedbacks,
        levelAssessment,
        false);
  }

  /** 최종 AI 호출 실패 시에도 수준 결과를 확정하기 위한 결정적 대체 결과를 만든다. */
  public static AiSessionFeedbackResult fallback(Long sessionId) {
    return new AiSessionFeedbackResult(
        sessionId,
        0,
        new BigDecimal("1.0"),
        "오늘의 대화를 끝까지 완료했어요.",
        "대화 내용을 바탕으로 현재 수준을 확인했어요.",
        List.of(),
        null,
        true);
  }
}
