// 구독을 결제한 스토어를 정의한다.

package com.landit.landitbe.feature.profile.domain;

import java.util.Optional;

/**
 * 구독을 결제한 스토어를 정의한다.
 *
 * <p>RevenueCat 웹훅의 store 값을 그대로 따른다. 웹은 이 값으로 스토어별 구독 관리 링크를 고른다.
 */
public enum SubscriptionStore {
  /** 애플 iOS App Store. */
  APP_STORE,
  /** 애플 macOS App Store. */
  MAC_APP_STORE,
  /** Google Play Store. */
  PLAY_STORE,
  /** Amazon Appstore. */
  AMAZON,
  /** Stripe 웹 결제. */
  STRIPE,
  /** RevenueCat 대시보드에서 부여한 프로모션 구독. */
  PROMOTIONAL,
  /** RevenueCat Billing 웹 결제. */
  RC_BILLING,
  /** Roku Channel Store. */
  ROKU,
  /** Paddle 웹 결제. */
  PADDLE,
  /** RevenueCat 테스트 스토어. */
  TEST_STORE;

  /**
   * RevenueCat store 문자열을 스토어로 변환한다.
   *
   * @param value RevenueCat store 값. null이면 빈 값
   * @return 대응하는 스토어. 알 수 없는 값이면 빈 값
   */
  public static Optional<SubscriptionStore> fromRevenueCat(String value) {
    if (value == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(valueOf(value));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
