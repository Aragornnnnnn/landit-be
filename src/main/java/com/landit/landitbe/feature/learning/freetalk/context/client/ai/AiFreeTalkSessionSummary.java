// AI에 전달할 프리톡 세션 요약 버전과 적용 범위를 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

/**
 * 요약 본문이 다룬 원문 sequence와 revision을 담는다.
 *
 * @param revision 저장된 요약 revision
 * @param coveredThroughSequence 기존 요약이 포함한 마지막 원문 순번
 * @param content 요약 본문
 */
public record AiFreeTalkSessionSummary(
    int revision, int coveredThroughSequence, AiFreeTalkSessionSummaryContent content) {}
