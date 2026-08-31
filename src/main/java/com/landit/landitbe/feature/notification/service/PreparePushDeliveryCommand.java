// 푸시 발송 이력을 선점하는 데 필요한 메시지 정보를 정의한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.NotificationContentVariant;
import com.landit.landitbe.feature.notification.domain.NotificationType;

/**
 * 푸시 발송 이력을 선점하는 데 필요한 메시지 정보를 정의한다.
 *
 * @param userProfileId 발송 대상 사용자 ID
 * @param userPushTokenId 발송 대상 사용자 Push Token ID
 * @param notificationType 알림 유형
 * @param contentVariant 알림 문구 변형. 정책이 없는 수동 발송은 {@code null}
 * @param deduplicationKey 중복 발송 방지 키
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 */
public record PreparePushDeliveryCommand(
    Long userProfileId,
    Long userPushTokenId,
    NotificationType notificationType,
    NotificationContentVariant contentVariant,
    String deduplicationKey,
    String title,
    String body,
    String deepLink) {

  /** 문구 변형이 없는 기존 발송 호출을 위한 명령을 생성한다. */
  public PreparePushDeliveryCommand(
      Long userProfileId,
      Long userPushTokenId,
      NotificationType notificationType,
      String deduplicationKey,
      String title,
      String body,
      String deepLink) {
    this(
        userProfileId,
        userPushTokenId,
        notificationType,
        null,
        deduplicationKey,
        title,
        body,
        deepLink);
  }
}
