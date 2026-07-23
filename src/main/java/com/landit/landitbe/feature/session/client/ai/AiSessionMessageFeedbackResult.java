// AI 세션 최종 피드백의 메시지별 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.FeedbackType;

/** AI 세션 최종 피드백의 메시지별 결과를 표현한다. */
public record AiSessionMessageFeedbackResult(
    Long messageId,
    FeedbackType feedbackType,
    String baseLocaleAnalogy,
    String positiveFeedback,
    String feedbackDetail,
    String correctionExpression,
    String correctionReason,
    String benchmarkMessage) {}
