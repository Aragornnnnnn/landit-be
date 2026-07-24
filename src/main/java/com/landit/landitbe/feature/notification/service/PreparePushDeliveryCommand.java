// 푸시 발송 이력을 선점하는 데 필요한 메시지 정보를 정의한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.NotificationType;

/**
 * 푸시 발송 이력을 선점하는 데 필요한 메시지 정보를 정의한다.
 *
 * @param userProfileId 발송 대상 사용자 ID
 * @param pushDeviceId 발송 대상 Push Device ID
 * @param notificationType 알림 유형
 * @param deduplicationKey 중복 발송 방지 키
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 */
public record PreparePushDeliveryCommand(
    Long userProfileId,
    Long pushDeviceId,
    NotificationType notificationType,
    String deduplicationKey,
    String title,
    String body,
    String deepLink) {}
