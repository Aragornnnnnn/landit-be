// 사용자 프로필에 저장된 구독 상태를 다른 기능에 전달한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.domain.SubscriptionStore;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import java.time.LocalDateTime;

/**
 * 사용자 프로필에 저장된 구독 상태를 다른 기능에 전달한다.
 *
 * @param subscriptionStatus 구독 상태
 * @param premium 프리미엄 혜택 적용 여부
 * @param periodType 현재 결제 기간 종류. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 * @param expiresAt 구독 만료 시각. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 * @param productId 구독 상품 ID. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 * @param store 결제한 스토어. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 */
public record UserSubscriptionSnapshot(
    SubscriptionStatus subscriptionStatus,
    boolean premium,
    SubscriptionPeriodType periodType,
    LocalDateTime expiresAt,
    String productId,
    SubscriptionStore store) {

  /**
   * 사용자 프로필의 구독 정보를 공개 계약으로 변환한다.
   *
   * @param userProfile 변환할 사용자 프로필
   * @return 사용자 구독 상태 스냅샷
   */
  public static UserSubscriptionSnapshot from(UserProfile userProfile) {
    return new UserSubscriptionSnapshot(
        userProfile.getSubscriptionStatus(),
        userProfile.isPremium(),
        userProfile.getSubscriptionPeriodType(),
        userProfile.getSubscriptionExpiresAt(),
        userProfile.getSubscriptionProductId(),
        userProfile.getSubscriptionStore());
  }
}
