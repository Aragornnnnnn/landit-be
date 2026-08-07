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
    List<AiSessionMessageFeedbackResult> messageFeedbacks) {}
