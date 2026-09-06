// 발송 직전 검증된 사용자 Push Token 정보를 전달한다.

package com.landit.landitbe.feature.notification.service;

/**
 * 발송 직전 검증된 사용자 Push Token 정보를 전달한다.
 *
 * @param userPushTokenId 사용자 Push Token ID
 * @param expoPushToken Expo Push Token
 */
public record UserPushTokenDeliveryTarget(Long userPushTokenId, String expoPushToken) {}
