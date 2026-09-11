// 유료 기능 접근 제한 판단에 필요한 사용자 상태와 허용 규칙을 담는다.

package com.landit.landitbe.feature.subscription.dto;

/**
 * 유료 기능 접근 제한 판단에 필요한 사용자 상태와 허용 규칙을 담는다.
 *
 * <p>유료 구독 도입 전({@code launched == false})에는 모든 기능을 허용한다. 도입 후에는 두 종류의 제한을 적용한다.
 *
 * <ul>
 *   <li>프리미엄 전용 기능(프리톡, 표현 학습, 발음 평가): {@code premium}이어야 한다.
 *   <li>시나리오 대화(세션 시작, 세션 메시지 전송): {@code premium}이거나, 도입 이후 시나리오 대화를 아직 끝까지 완료하지 않았어야 한다. 무료 사용자는
 *       도입 후 첫 시나리오 하나만 끝까지 할 수 있다.
 * </ul>
 *
 * @param launched 유료 구독 도입 시점에 도달해 제한을 적용하는지
 * @param premium 프리미엄 혜택 적용 여부
 * @param conversationCompletedSinceLaunch 도입 이후 시나리오 대화를 끝까지 완료한 적이 있는지
 */
public record PremiumAccess(
    boolean launched, boolean premium, boolean conversationCompletedSinceLaunch) {

  /**
   * 유료 구독 도입 전 상태를 만든다. 모든 기능이 허용된다.
   *
   * @return 도입 전 접근 판단 결과
   */
  public static PremiumAccess beforeLaunch() {
    return new PremiumAccess(false, false, false);
  }

  /**
   * 유료 구독 도입 후 상태를 만든다.
   *
   * @param premium 프리미엄 혜택 적용 여부
   * @param conversationCompletedSinceLaunch 도입 이후 시나리오 대화 완료 여부
   * @return 도입 후 접근 판단 결과
   */
  public static PremiumAccess afterLaunch(
      boolean premium, boolean conversationCompletedSinceLaunch) {
    return new PremiumAccess(true, premium, conversationCompletedSinceLaunch);
  }

  /**
   * 프리미엄 전용 기능을 쓸 수 있는지 확인한다.
   *
   * @return 도입 전이거나 프리미엄이면 {@code true}
   */
  public boolean allowsPremiumOnlyFeature() {
    return !launched || premium;
  }

  /**
   * 시나리오 대화(세션 시작·메시지 전송)를 할 수 있는지 확인한다.
   *
   * @return 도입 전이거나 프리미엄이거나, 아직 도입 이후 대화를 완료하지 않았으면 {@code true}
   */
  public boolean allowsScenarioConversation() {
    return !launched || premium || !conversationCompletedSinceLaunch;
  }
}
