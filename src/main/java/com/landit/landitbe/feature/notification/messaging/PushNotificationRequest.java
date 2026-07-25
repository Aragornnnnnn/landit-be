// Queue에 발행할 사용자별 푸시 알림 내용을 정의한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import java.time.Instant;

/**
 * Queue에 발행할 사용자별 푸시 알림 내용을 정의한다.
 *
 * @param eventId 동일 이벤트의 중복 발송을 막을 식별자
 * @param userProfileId 발송 대상 사용자 ID
 * @param notificationType 알림 유형
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 * @param occurredAt 알림 이벤트 발생 시각
 */
public record PushNotificationRequest(
    String eventId,
    Long userProfileId,
    NotificationType notificationType,
    String title,
    String body,
    String deepLink,
    Instant occurredAt) {}
