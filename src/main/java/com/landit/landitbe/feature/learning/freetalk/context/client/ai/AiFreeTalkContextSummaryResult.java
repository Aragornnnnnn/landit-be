// AI가 생성한 프리톡 세션 요약 결과를 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

/** 요약 경계와 검증된 요약 본문을 담는다. */
public record AiFreeTalkContextSummaryResult(
    String policyVersion,
    int baseRevision,
    int coveredThroughSequence,
    AiFreeTalkSessionSummaryContent summary) {}
