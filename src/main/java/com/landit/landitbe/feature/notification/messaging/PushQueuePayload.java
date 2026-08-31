// Push Queue 메시지 유형별 선택 payload를 정의한다.

package com.landit.landitbe.feature.notification.messaging;

/**
 * Push Queue 메시지 유형별 선택 payload를 정의한다.
 *
 * @param pushDeliveryId Receipt를 확인할 Push Delivery ID
 * @param receiptAttempt Receipt 확인 시도 횟수
 */
public record PushQueuePayload(Long pushDeliveryId, Integer receiptAttempt) {}
