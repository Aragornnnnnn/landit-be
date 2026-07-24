// Receipt 확인 메시지를 Push 전용 Queue에 지연 발행하는 Port다.

package com.landit.landitbe.feature.notification.messaging;

/** Receipt 확인 메시지를 Push 전용 Queue에 지연 발행하는 Port다. */
public interface PushQueuePublisher {

  /**
   * Expo Receipt 확인 메시지를 지정된 초기 지연으로 발행한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param attempt Receipt 확인 시도 횟수
   */
  void scheduleReceiptCheck(Long pushDeliveryId, int attempt);
}
