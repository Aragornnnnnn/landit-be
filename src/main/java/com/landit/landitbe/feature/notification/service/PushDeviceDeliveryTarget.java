// 발송 직전 잠금 확인된 Push Device 식별자와 Token을 전달한다.

package com.landit.landitbe.feature.notification.service;

/**
 * 발송 직전 잠금 확인된 Push Device 식별자와 Token을 전달한다.
 *
 * @param pushDeviceId Push Device ID
 * @param expoPushToken 현재 발송 가능한 Expo Push Token
 */
public record PushDeviceDeliveryTarget(Long pushDeviceId, String expoPushToken) {}
