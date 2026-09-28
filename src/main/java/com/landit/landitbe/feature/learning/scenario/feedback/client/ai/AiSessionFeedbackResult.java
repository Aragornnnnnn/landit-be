// AI 세션 최종 피드백 생성 결과를 표현한다.

package com.landit.landitbe.feature.learning.scenario.feedback.client.ai;

import com.landit.landitbe.feature.learning.scenario.assessment.client.ai.AiSessionLevelAssessment;
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
 * @param growthFeedback AI가 제안한 직전 교정 비교 근거
 * @param usedExpressions AI가 제안한 배운 표현 사용 근거
 * @param generationFallback 결정적 대체 응답 여부
 */
public record AiSessionFeedbackResult(
    Long sessionId,
    int nativeScore,
    BigDecimal starRating,
    String highlightMessage,
    String summaryMessage,
    List<AiSessionMessageFeedbackResult> messageFeedbacks,
    AiSessionLevelAssessment levelAssessment,
    ScenarioGrowthFeedback growthFeedback,
    List<UsedExpression> usedExpressions,
    boolean generationFallback) {

  /**
   * 수준 평가가 없는 기존 클라이언트 결과를 만든다.
   *
   * @param sessionId 학습 세션 ID
   * @param nativeScore 원어민 관점 점수
   * @param starRating 세션 별점
   * @param highlightMessage 최종 피드백 강조 메시지
   * @param summaryMessage 최종 피드백 요약
   * @param messageFeedbacks 메시지별 피드백 목록
   */
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
        null,
        List.of(),
        false);
  }

  /**
   * 기존 결과 구성 경로와 대체 응답의 호환성을 유지한다.
   *
   * @param sessionId 학습 세션 ID
   * @param nativeScore 원어민 관점 점수
   * @param starRating 세션 별점
   * @param highlightMessage 최종 피드백 강조 메시지
   * @param summaryMessage 최종 피드백 요약
   * @param messageFeedbacks 메시지별 피드백 목록
   * @param levelAssessment 사용자 수준 평가 결과
   * @param generationFallback 결정적 대체 응답 여부
   */
  public AiSessionFeedbackResult(
      Long sessionId,
      int nativeScore,
      BigDecimal starRating,
      String highlightMessage,
      String summaryMessage,
      List<AiSessionMessageFeedbackResult> messageFeedbacks,
      AiSessionLevelAssessment levelAssessment,
      boolean generationFallback) {
    this(
        sessionId,
        nativeScore,
        starRating,
        highlightMessage,
        summaryMessage,
        messageFeedbacks,
        levelAssessment,
        null,
        List.of(),
        generationFallback);
  }

  /**
   * 정상 생성된 수준 평가를 포함한 결과를 만든다.
   *
   * @param sessionId 학습 세션 ID
   * @param nativeScore 원어민 관점 점수
   * @param starRating 세션 별점
   * @param highlightMessage 최종 피드백 강조 메시지
   * @param summaryMessage 최종 피드백 요약
   * @param messageFeedbacks 메시지별 피드백 목록
   * @param levelAssessment 사용자 수준 평가 결과
   */
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
        null,
        List.of(),
        false);
  }

  /**
   * 원격 AI 응답의 비교·표현 결과를 함께 담는다.
   *
   * @param sessionId 학습 세션 ID
   * @param nativeScore 원어민 관점 점수
   * @param starRating 세션 별점
   * @param highlightMessage 최종 피드백 강조 메시지
   * @param summaryMessage 최종 피드백 요약
   * @param messageFeedbacks 메시지별 피드백 목록
   * @param growthFeedback 직전 교정 비교 결과
   * @param usedExpressions 배운 표현 사용 결과
   */
  public AiSessionFeedbackResult(
      Long sessionId,
      int nativeScore,
      BigDecimal starRating,
      String highlightMessage,
      String summaryMessage,
      List<AiSessionMessageFeedbackResult> messageFeedbacks,
      ScenarioGrowthFeedback growthFeedback,
      List<UsedExpression> usedExpressions) {
    this(
        sessionId,
        nativeScore,
        starRating,
        highlightMessage,
        summaryMessage,
        messageFeedbacks,
        null,
        growthFeedback,
        usedExpressions,
        false);
  }

  /**
   * 최종 AI 호출 실패 시에도 수준 결과를 확정하기 위한 결정적 대체 결과를 만든다.
   *
   * @param sessionId 학습 세션 ID
   * @return 요약 대체 응답
   */
  public static AiSessionFeedbackResult fallback(Long sessionId) {
    return new AiSessionFeedbackResult(
        sessionId,
        0,
        new BigDecimal("1.0"),
        "오늘의 대화를 끝까지 완료했어요.",
        "대화 내용을 바탕으로 현재 수준을 확인했어요.",
        List.of(),
        null,
        null,
        List.of(),
        true);
  }

  /**
   * AI가 제안한 직전 교정과 현재 문장의 비교 정보다.
   *
   * @param pattern 비교할 문법 실수 유형
   * @param previousMessageId 직전 사용자 메시지 ID
   * @param previousSentence 직전 발화 중 비교할 문장
   * @param previousWrongSpan 직전 발화에서 틀린 구절
   * @param currentMessageId 현재 사용자 메시지 ID
   * @param currentSentence 현재 발화 중 비교할 문장
   * @param currentSpan 현재 발화의 비교 구절
   * @param succeeded 현재 발화에서 해당 패턴을 올바르게 사용했는지
   */
  public record ScenarioGrowthFeedback(
      com.landit.landitbe.feature.learning.scenario.feedback.domain.ScenarioMistakePattern pattern,
      Long previousMessageId,
      String previousSentence,
      String previousWrongSpan,
      Long currentMessageId,
      String currentSentence,
      String currentSpan,
      Boolean succeeded) {}

  /**
   * AI가 실제 사용했다고 제안한 학습 표현이다.
   *
   * @param expressionId 학습 표현 ID
   * @param messageId 표현을 사용한 사용자 메시지 ID
   * @param matchedText 메시지에서 표현에 해당하는 정확한 구절
   */
  public record UsedExpression(Long expressionId, Long messageId, String matchedText) {}
}
