// 프리톡 세션 요약 생성에 필요한 원문 구간을 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

import java.util.List;

/** AI가 요약할 원문 구간과 기존 요약의 적용 범위를 담는다. */
public record AiFreeTalkContextSummaryRequest(
    Long sessionId,
    String policyVersion,
    int baseRevision,
    AiFreeTalkSessionSummaryContent previousSummary,
    int coveredThroughSequence,
    int targetThroughSequence,
    String timezone,
    List<AiFreeTalkContextSummarySourceMessage> sourceMessages) {
  /** 목록을 방어적으로 복사한다. */
  public AiFreeTalkContextSummaryRequest {
    sourceMessages = sourceMessages == null ? List.of() : List.copyOf(sourceMessages);
  }
}
