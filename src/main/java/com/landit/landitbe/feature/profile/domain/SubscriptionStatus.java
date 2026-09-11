// 사용자 프리미엄 구독 상태를 정의한다.

package com.landit.landitbe.feature.profile.domain;

/**
 * 사용자 프리미엄 구독 상태를 정의한다.
 *
 * <p>Google Play 구독 상태 모델(ACTIVE, CANCELED, EXPIRED)을 따르고, 구독 이력이 없는 사용자를 위해 NONE을 추가했다. RevenueCat
 * 웹훅 이벤트와 다음처럼 대응한다.
 *
 * <ul>
 *   <li>INITIAL_PURCHASE, RENEWAL, UNCANCELLATION → {@link #ACTIVE}
 *   <li>CANCELLATION → {@link #CANCELED}. 단, cancel_reason이 CUSTOMER_SUPPORT(환불)이면 {@link #EXPIRED}
 *   <li>EXPIRATION → {@link #EXPIRED}
 * </ul>
 *
 * <p>프리미엄 혜택 적용 여부는 상태 이름이 아니라 {@link #isPremium()}으로 판단한다. {@link #CANCELED}는 이름과 달리 만료 전까지 {@code
 * isPremium() == true}인 상태다.
 */
public enum SubscriptionStatus {
  /** 구독한 적이 없다. 신규 가입 사용자의 기본값이며 {@code isPremium() == false}다. */
  NONE,
  /** 구독 중이고 자동 갱신이 켜져 있다. {@code isPremium() == true}다. */
  ACTIVE,
  /**
   * 해지를 예약했다. 자동 갱신은 꺼졌지만 이미 결제한 기간이 끝날 때까지 {@code isPremium() == true}다. 만료 시각이 지나면 RevenueCat이
   * EXPIRATION 이벤트를 보내 {@link #EXPIRED}로 바뀌고, 사용자가 해지를 철회하면 UNCANCELLATION 이벤트로 {@link #ACTIVE}로
   * 돌아간다.
   */
  CANCELED,
  /** 구독 기간이 만료됐거나 환불로 종료됐다. {@code isPremium() == false}이며, 다시 결제하면 {@link #ACTIVE}로 돌아간다. */
  EXPIRED;

  /**
   * 프리미엄 혜택이 켜진 상태인지 확인한다.
   *
   * @return {@link #ACTIVE} 또는 {@link #CANCELED}면 {@code true}
   */
  public boolean isPremium() {
    return this == ACTIVE || this == CANCELED;
  }
}
