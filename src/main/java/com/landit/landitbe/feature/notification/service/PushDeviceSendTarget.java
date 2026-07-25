// 복습 리마인더를 보낼 앱 설치의 사용자와 기기 식별자를 전달한다.

package com.landit.landitbe.feature.notification.service;

/**
 * 복습 리마인더를 보낼 앱 설치의 사용자와 기기 식별자를 전달한다.
 *
 * @param userProfileId 발송 대상 사용자 ID
 * @param pushDeviceId 발송 대상 Push Device ID
 */
public record PushDeviceSendTarget(Long userProfileId, Long pushDeviceId) {}
