// 알림 발송 직전 필요한 활성 사용자 구독과 연락처 정보를 전달한다.

package com.landit.landitbe.feature.profile.subscription.dto;

/**
 * 활성 사용자에게만 제공하는 알림 대상 정보다.
 *
 * @param subscription 현재 구독 상태
 * @param email 가입 이메일. 없으면 null
 * @param pushGranted 저장된 푸시 권한 허용 여부
 */
public record SubscriptionNotificationTarget(
    UserSubscriptionSnapshot subscription, String email, boolean pushGranted) {}
