// 한 사용자에게 보낼 푸시 알림의 이벤트와 표시 내용을 전달한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.NotificationContentVariant;
import com.landit.landitbe.feature.notification.domain.NotificationType;

/**
 * 한 사용자에게 보낼 푸시 알림의 이벤트와 표시 내용을 전달한다.
 *
 * @param eventId 동일 이벤트의 중복 발송을 막을 식별자
 * @param userProfileId 발송 대상 사용자 ID
 * @param notificationType 알림 유형
 * @param contentVariant 알림 문구 변형. 정책이 없는 수동 발송은 {@code null}
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 */
public record SendPushNotificationCommand(
    String eventId,
    Long userProfileId,
    NotificationType notificationType,
    NotificationContentVariant contentVariant,
    String title,
    String body,
    String deepLink) {

  /** 문구 변형이 없는 기존 발송 호출을 위한 명령을 생성한다. */
  public SendPushNotificationCommand(
      String eventId,
      Long userProfileId,
      NotificationType notificationType,
      String title,
      String body,
      String deepLink) {
    this(eventId, userProfileId, notificationType, null, title, body, deepLink);
  }
}
