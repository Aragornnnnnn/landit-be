// 선점된 푸시 발송 이력과 외부 제공자 요청 정보를 정의한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.client.PushMessage;

/**
 * 선점된 푸시 발송 이력과 외부 제공자 요청 정보를 정의한다.
 *
 * @param pushDeliveryId Push Delivery ID
 * @param expoPushToken Expo Push Token
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 */
public record PreparedPushDelivery(
    Long pushDeliveryId, String expoPushToken, String title, String body, String deepLink) {

  /**
   * Expo 발송 Port에 전달할 메시지로 변환한다.
   *
   * @return Expo Push 발송 메시지
   */
  public PushMessage toPushMessage() {
    return new PushMessage(expoPushToken, title, body, deepLink);
  }
}
