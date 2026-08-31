// Push Queue 메시지 유형별 선택 payload를 정의한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.feature.notification.domain.NotificationType;

/**
 * Push Queue 메시지 유형별 선택 payload를 정의한다.
 *
 * @param userProfileId 알림을 받을 사용자 ID
 * @param notificationType 알림 유형
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 * @param pushDeliveryId Receipt를 확인할 Push Delivery ID
 * @param receiptAttempt Receipt 확인 시도 횟수
 */
public record PushQueuePayload(
    Long userProfileId,
    NotificationType notificationType,
    String title,
    String body,
    String deepLink,
    Long pushDeliveryId,
    Integer receiptAttempt) {

  /**
   * 사용자별 푸시 발송 payload를 생성한다.
   *
   * @param request 발송 대상과 알림 내용
   * @return 푸시 발송 payload
   */
  public static PushQueuePayload notification(PushNotificationRequest request) {
    return new PushQueuePayload(
        request.userProfileId(),
        request.notificationType(),
        request.title(),
        request.body(),
        request.deepLink(),
        null,
        null);
  }

  /**
   * Receipt 확인 payload를 생성한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param receiptAttempt Receipt 확인 시도 횟수
   * @return Receipt 확인 payload
   */
  public static PushQueuePayload receipt(Long pushDeliveryId, int receiptAttempt) {
    return new PushQueuePayload(null, null, null, null, null, pushDeliveryId, receiptAttempt);
  }
}
