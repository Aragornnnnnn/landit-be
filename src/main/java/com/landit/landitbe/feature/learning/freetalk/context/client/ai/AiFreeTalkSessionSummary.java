// AI에 전달할 프리톡 세션 요약 버전과 적용 범위를 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

/** 요약 본문이 다룬 원문 sequence와 revision을 담는다. */
public record AiFreeTalkSessionSummary(
    int revision, int coveredThroughSequence, AiFreeTalkSessionSummaryContent content) {}
