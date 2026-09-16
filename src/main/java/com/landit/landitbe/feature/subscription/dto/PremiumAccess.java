// 유료 기능 접근 제한 판단에 필요한 사용자 상태와 허용 규칙을 담는다.

package com.landit.landitbe.feature.subscription.dto;

/**
 * 유료 기능 접근 제한 판단에 필요한 사용자 상태와 허용 규칙을 담는다.
 *
 * <p>유료 구독 도입 전({@code launched == false})에는 모든 기능을 허용한다. 도입 후에는 프리미엄 전용 기능(프리톡, 표현 학습, 발음 평가)을
 * {@code premium}인 사용자에게만 허용한다. 시나리오 대화(세션 시작, 메시지 전송, 속마음 조회)는 구독과 관계없이 제한하지 않으며, 무료 사용자의 상세 피드백
 * 잠금은 피드백 응답에서 따로 판단한다.
 *
 * @param launched 유료 구독 도입 시점에 도달해 제한을 적용하는지
 * @param premium 프리미엄 혜택 적용 여부
 */
public record PremiumAccess(boolean launched, boolean premium) {

  /**
   * 유료 구독 도입 전 상태를 만든다. 모든 기능이 허용된다.
   *
   * @return 도입 전 접근 판단 결과
   */
  public static PremiumAccess beforeLaunch() {
    return new PremiumAccess(false, false);
  }

  /**
   * 유료 구독 도입 후 상태를 만든다.
   *
   * @param premium 프리미엄 혜택 적용 여부
   * @return 도입 후 접근 판단 결과
   */
  public static PremiumAccess afterLaunch(boolean premium) {
    return new PremiumAccess(true, premium);
  }

  /**
   * 프리미엄 전용 기능을 쓸 수 있는지 확인한다.
   *
   * @return 도입 전이거나 프리미엄이면 {@code true}
   */
  public boolean allowsPremiumOnlyFeature() {
    return !launched || premium;
  }
}
