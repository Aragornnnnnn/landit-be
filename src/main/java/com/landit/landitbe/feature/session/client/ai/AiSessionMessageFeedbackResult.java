// AI 세션 최종 피드백의 메시지별 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.FeedbackType;

/**
 * AI 세션 최종 피드백의 메시지별 결과를 표현한다.
 *
 * @param messageId 메시지 ID
 * @param feedbackType 피드백 유형
 * @param baseLocaleAnalogy 기준 언어 비유 설명
 * @param positiveFeedback 긍정 피드백
 * @param feedbackDetail 상세 피드백
 * @param correctionExpression 교정 표현
 * @param correctionReason 교정 사유
 * @param benchmarkMessage 비교용 모범 메시지
 */
public record AiSessionMessageFeedbackResult(
    Long messageId,
    FeedbackType feedbackType,
    String baseLocaleAnalogy,
    String positiveFeedback,
    String feedbackDetail,
    String correctionExpression,
    String correctionReason,
    String benchmarkMessage) {}
