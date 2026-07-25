// Receipt 확인 메시지를 Push 전용 Queue에 지연 발행하는 Port다.

package com.landit.landitbe.feature.notification.messaging;

/** 푸시 발송과 Receipt 확인 메시지를 Push 전용 Queue에 발행하는 Port다. */
public interface PushQueuePublisher {

  /**
   * 사용자별 푸시 발송 메시지를 Push 전용 Queue에 즉시 발행한다.
   *
   * @param request 발송 대상과 알림 내용
   */
  void publishNotification(PushNotificationRequest request);

  /**
   * Expo Receipt 확인 메시지를 지정된 초기 지연으로 발행한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param attempt Receipt 확인 시도 횟수
   */
  void scheduleReceiptCheck(Long pushDeliveryId, int attempt);
}
