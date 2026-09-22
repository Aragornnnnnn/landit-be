// AI 요약에 전달할 프리톡 원문 메시지와 발생 시각을 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

import java.time.OffsetDateTime;

/**
 * 요약 대상 원문 메시지의 순서와 역할을 담는다.
 *
 * @param sequence 세션 내 원문 메시지 순번
 * @param messageId 원문 메시지 ID
 * @param turnNumber 원문 메시지의 턴 번호
 * @param role 원문 발화자 역할
 * @param content 원문 메시지 내용
 * @param occurredAt 시간대가 포함된 원문 발생 시각
 */
public record AiFreeTalkContextSummarySourceMessage(
    int sequence,
    Long messageId,
    int turnNumber,
    String role,
    String content,
    OffsetDateTime occurredAt) {}
