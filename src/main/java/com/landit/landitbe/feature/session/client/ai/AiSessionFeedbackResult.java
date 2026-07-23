// AI 세션 최종 피드백 생성 결과를 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import java.math.BigDecimal;
import java.util.List;

/** AI 세션 최종 피드백 생성 결과를 표현한다. */
public record AiSessionFeedbackResult(
    Long sessionId,
    int nativeScore,
    BigDecimal starRating,
    String highlightMessage,
    String summaryMessage,
    List<AiSessionMessageFeedbackResult> messageFeedbacks) {}
