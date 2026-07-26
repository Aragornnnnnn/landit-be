// Push Queue 메시지 유형별 선택 payload를 정의한다.

package com.landit.landitbe.feature.notification.messaging;

/**
 * Push Receipt 확인 메시지의 payload를 정의한다.
 *
 * @param pushDeliveryId Receipt를 확인할 Push Delivery ID
 * @param receiptAttempt Receipt 확인 시도 횟수
 */
public record PushQueuePayload(Long pushDeliveryId, Integer receiptAttempt) {

  /**
   * Receipt 확인 payload를 생성한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param receiptAttempt Receipt 확인 시도 횟수
   * @return Receipt 확인 payload
   */
  public static PushQueuePayload receipt(Long pushDeliveryId, int receiptAttempt) {
    return new PushQueuePayload(pushDeliveryId, receiptAttempt);
  }
}
