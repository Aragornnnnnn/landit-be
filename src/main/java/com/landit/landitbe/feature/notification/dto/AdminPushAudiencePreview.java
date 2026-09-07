// 캠페인의 조회 시점 예상 사용자 수와 토큰 수를 전달한다.

package com.landit.landitbe.feature.notification.dto;

import java.time.LocalDateTime;

/**
 * 확정 전 예상 발송 대상이다.
 *
 * @param estimatedUserCount 활성 토큰을 보유한 활성 사용자 수
 * @param estimatedTokenCount 활성 토큰 수
 * @param estimatedAt 예상치를 조회한 시각
 */
public record AdminPushAudiencePreview(
    long estimatedUserCount, long estimatedTokenCount, LocalDateTime estimatedAt) {}
