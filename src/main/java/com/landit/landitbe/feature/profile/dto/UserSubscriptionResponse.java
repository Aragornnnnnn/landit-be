// 사용자의 서버 기준 구독 상태를 API 응답으로 제공한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 사용자의 서버 기준 구독 상태를 API 응답으로 제공한다.
 *
 * @param subscriptionStatus 구독 상태
 * @param premium 프리미엄 혜택 적용 여부
 * @param expiresAt 구독 만료 시각. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 */
@Schema(description = "사용자 구독 상태")
public record UserSubscriptionResponse(
    @Schema(
            description =
                "구독 상태. NONE(구독 이력 없음), ACTIVE(구독 중), CANCELED(해지 예약, 만료 전까지 프리미엄 유지),"
                    + " EXPIRED(만료 또는 환불). 프리미엄 적용 여부는 premium으로 판단한다.",
            example = "ACTIVE")
        SubscriptionStatus subscriptionStatus,
    @Schema(description = "프리미엄 혜택 적용 여부", example = "true") boolean premium,
    @Schema(description = "구독 만료 시각. 프리미엄이 꺼져 있으면 null", example = "2026-10-04T12:00:00")
        LocalDateTime expiresAt) {

  /**
   * 사용자 프로필의 구독 정보를 응답으로 변환한다.
   *
   * @param userProfile 변환할 사용자 프로필
   * @return 사용자 구독 상태 응답
   */
  public static UserSubscriptionResponse from(UserProfile userProfile) {
    return new UserSubscriptionResponse(
        userProfile.getSubscriptionStatus(),
        userProfile.isPremium(),
        userProfile.getSubscriptionExpiresAt());
  }
}
