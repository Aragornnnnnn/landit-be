// 결제 제공자 이벤트로 갱신할 구독 정보를 전달한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import java.time.LocalDateTime;

/**
 * 결제 제공자 이벤트로 갱신할 구독 정보를 전달한다.
 *
 * @param status 갱신할 구독 상태
 * @param periodType 현재 결제 기간 종류. 프리미엄이 꺼지거나 알 수 없으면 null
 * @param expiresAt 구독 만료 시각. 프리미엄이 꺼지거나 알 수 없으면 null
 * @param eventAt 이벤트 발생 시각
 */
public record SubscriptionUpdateCommand(
    SubscriptionStatus status,
    SubscriptionPeriodType periodType,
    LocalDateTime expiresAt,
    LocalDateTime eventAt) {}
