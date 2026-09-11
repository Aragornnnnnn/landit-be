// 결제 이력으로 저장하는 RevenueCat 구독 이벤트 타입을 정의한다.

package com.landit.landitbe.feature.subscription.domain;

import java.util.Optional;

/**
 * 결제 이력으로 저장하는 RevenueCat 구독 이벤트 타입을 정의한다.
 *
 * <p>RevenueCat 웹훅의 type 값을 그대로 따른다. 여기 없는 타입(TEST, TRANSFER 등)은 이력으로 저장하지 않는다.
 */
public enum SubscriptionEventType {
  /** 첫 결제 또는 무료 체험 시작. */
  INITIAL_PURCHASE,
  /** 갱신 결제. 무료 체험이 끝나고 처음 결제된 경우도 포함한다. */
  RENEWAL,
  /** 해지 예약 또는 환불. 환불이면 cancel_reason이 CUSTOMER_SUPPORT다. */
  CANCELLATION,
  /** 해지 예약 취소. */
  UNCANCELLATION,
  /** 구독 만료. */
  EXPIRATION,
  /** 결제 실패. */
  BILLING_ISSUE,
  /** 플랜 변경. */
  PRODUCT_CHANGE;

  /**
   * RevenueCat type 문자열을 이벤트 타입으로 변환한다.
   *
   * @param value RevenueCat type 값. null이면 빈 값
   * @return 대응하는 이벤트 타입. 이력으로 저장하지 않는 타입이면 빈 값
   */
  public static Optional<SubscriptionEventType> fromRevenueCat(String value) {
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
