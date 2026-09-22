// 결제 이력으로 저장하는 RevenueCat 구독 이벤트 타입을 정의한다.

package com.landit.landitbe.feature.subscription.event.domain;

import java.util.Optional;

/**
 * 결제 이력으로 저장하는 RevenueCat 구독 이벤트 타입을 정의한다.
 *
 * <p>RevenueCat 웹훅의 type 값을 그대로 따른다. 여기 없는 타입(TEST 등)은 이력으로 저장하지 않는다.
 */
public enum SubscriptionEventType {
  /** 첫 결제 또는 무료 체험 시작. */
  INITIAL_PURCHASE,
  /** 갱신 결제. 무료 체험이 끝나고 처음 결제된 경우도 포함한다. */
  RENEWAL,
  /**
   * 해지 예약 또는 환불. 환불이면 cancel_reason이 CUSTOMER_SUPPORT다. 갱신 결제 실패 시에는 BILLING_ISSUE와 함께
   * cancel_reason이 BILLING_ERROR로 오며 이때는 구독 상태를 바꾸지 않는다.
   */
  CANCELLATION,
  /** 해지 예약 취소. */
  UNCANCELLATION,
  /** 구독 만료. */
  EXPIRATION,
  /** 갱신 결제 실패. 스토어 유예 기간이 있으면 grace_period_expiration_at_ms로 유예 종료 시각이 함께 오고, 그때까지 프리미엄을 유지한다. */
  BILLING_ISSUE,
  /** 플랜 변경. */
  PRODUCT_CHANGE,
  /**
   * 자동 갱신 없는 일회성 구매. RevenueCat 대시보드에서 프로모션 권한을 직접 부여하면 INITIAL_PURCHASE 대신 이 타입으로 오고, store와
   * period_type이 모두 PROMOTIONAL이다. 기간이 끝나면 EXPIRATION이 온다.
   */
  NON_RENEWING_PURCHASE,
  /** 구독이 다른 앱 계정으로 이전됨. 넘겨받은 계정의 이력에만 남긴다. */
  TRANSFER;

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
