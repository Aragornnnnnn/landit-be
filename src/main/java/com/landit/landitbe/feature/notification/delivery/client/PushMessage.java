// 외부 Push 제공자에 전달할 Token과 사용자 노출 메시지를 정의한다.

package com.landit.landitbe.feature.notification.delivery.client;

/**
 * 외부 Push 제공자에 전달할 Token과 사용자 노출 메시지를 정의한다.
 *
 * @param expoPushToken Expo Push Token
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 * @param userProfileId 딥링크를 열 수 있는 계정 ID
 */
public record PushMessage(
    String expoPushToken, String title, String body, String deepLink, Long userProfileId) {
  /** 기존 호출의 호환성을 유지한다. */
  public PushMessage(String expoPushToken, String title, String body, String deepLink) {
    this(expoPushToken, title, body, deepLink, null);
  }
}
