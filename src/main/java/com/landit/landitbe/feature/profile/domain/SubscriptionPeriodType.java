// 현재 구독 결제 기간의 종류를 정의한다.

package com.landit.landitbe.feature.profile.domain;

import java.util.Optional;

/**
 * 현재 구독 결제 기간의 종류를 정의한다.
 *
 * <p>구독 상태({@link SubscriptionStatus})와 별개의 축이다. 예를 들어 무료 체험 중에 해지를 예약하면 상태는 CANCELED, 기간 종류는
 * TRIAL이 된다. RevenueCat 웹훅의 period_type 값을 그대로 따른다.
 */
public enum SubscriptionPeriodType {
  /** 무료 체험 기간이다. 체험이 끝나고 결제되면 RENEWAL 이벤트로 {@link #NORMAL}이 된다. */
  TRIAL,
  /** 스토어 할인가가 적용되는 도입 기간이다. */
  INTRO,
  /** 정가 결제 기간이다. */
  NORMAL,
  /** 스토어 프로모션 코드나 오퍼로 무료 제공된 기간이다. */
  PROMOTIONAL,
  /** 자동 갱신 없이 선결제한 기간이다. */
  PREPAID;

  /**
   * RevenueCat period_type 문자열을 기간 종류로 변환한다.
   *
   * @param value RevenueCat period_type 값. null이면 빈 값
   * @return 대응하는 기간 종류. 알 수 없는 값이면 빈 값
   */
  public static Optional<SubscriptionPeriodType> fromRevenueCat(String value) {
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
