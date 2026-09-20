// AI 요약에 전달할 프리톡 원문 메시지와 발생 시각을 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

import java.time.OffsetDateTime;

/** 요약 대상 원문 메시지의 순서와 역할을 담는다. */
public record AiFreeTalkContextSummarySourceMessage(
    int sequence,
    Long messageId,
    int turnNumber,
    String role,
    String content,
    OffsetDateTime occurredAt) {}
