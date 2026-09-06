// Scheduler와 API가 Push 전용 Queue에 발행하는 메시지 계약을 정의한다.

package com.landit.landitbe.feature.notification.messaging;

import java.time.Instant;

/**
 * Scheduler와 API가 Push 전용 Queue에 발행하는 메시지 계약을 정의한다.
 *
 * @param version 메시지 계약 버전
 * @param messageId 발행자가 생성한 비어 있지 않은 메시지 ID
 * @param messageType Push 메시지 유형
 * @param occurredAt 메시지 기준 시각
 * @param payload 메시지 유형별 payload
 */
public record PushQueueMessage(
    int version,
    String messageId,
    String messageType,
    Instant occurredAt,
    PushQueuePayload payload) {

  public static final String MAILBOX_REPLY_NOTIFICATION_BATCH = "MAILBOX_REPLY_NOTIFICATION_BATCH";
  public static final String PUSH_RECEIPT_CHECK = "PUSH_RECEIPT_CHECK";
}
