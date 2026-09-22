// 프리톡 세션 요약 생성에 필요한 원문 구간을 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

import java.util.List;

/**
 * AI가 요약할 원문 구간과 기존 요약의 적용 범위를 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param policyVersion 적용할 요약 정책 버전
 * @param baseRevision 요약을 갱신하기 전 revision
 * @param previousSummary 직전 요약 본문. 최초 요약이면 null
 * @param coveredThroughSequence 기존 요약이 포함한 마지막 원문 순번
 * @param targetThroughSequence 이번에 요약할 마지막 원문 순번
 * @param timezone 원문 시각 해석에 사용할 IANA 시간대
 * @param sourceMessages 이번 요약 대상 원문 메시지
 */
public record AiFreeTalkContextSummaryRequest(
    Long sessionId,
    String policyVersion,
    int baseRevision,
    AiFreeTalkSessionSummaryContent previousSummary,
    int coveredThroughSequence,
    int targetThroughSequence,
    String timezone,
    List<AiFreeTalkContextSummarySourceMessage> sourceMessages) {
  /**
   * 목록을 방어적으로 복사한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param policyVersion 적용할 요약 정책 버전
   * @param baseRevision 요약을 갱신하기 전 revision
   * @param previousSummary 직전 요약 본문. 최초 요약이면 null
   * @param coveredThroughSequence 기존 요약이 포함한 마지막 원문 순번
   * @param targetThroughSequence 이번에 요약할 마지막 원문 순번
   * @param timezone 원문 시각 해석에 사용할 IANA 시간대
   * @param sourceMessages 이번 요약 대상 원문 메시지
   */
  public AiFreeTalkContextSummaryRequest {
    sourceMessages = sourceMessages == null ? List.of() : List.copyOf(sourceMessages);
  }
}
