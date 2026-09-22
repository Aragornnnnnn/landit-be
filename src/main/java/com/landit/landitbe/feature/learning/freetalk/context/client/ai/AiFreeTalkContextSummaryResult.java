// AI가 생성한 프리톡 세션 요약 결과를 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

/**
 * 요약 경계와 검증된 요약 본문을 담는다.
 *
 * @param policyVersion 적용할 요약 정책 버전
 * @param baseRevision 요약을 갱신하기 전 revision
 * @param coveredThroughSequence 기존 요약이 포함한 마지막 원문 순번
 * @param summary 생성된 요약 본문
 */
public record AiFreeTalkContextSummaryResult(
    String policyVersion,
    int baseRevision,
    int coveredThroughSequence,
    AiFreeTalkSessionSummaryContent summary) {}
