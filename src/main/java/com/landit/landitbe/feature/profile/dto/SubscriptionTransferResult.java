// 계정 간 구독 이전 요청이 어떻게 처리됐는지와 옮겨진 구독 정보를 전달한다.

package com.landit.landitbe.feature.profile.dto;

/**
 * 계정 간 구독 이전 요청이 어떻게 처리됐는지와 옮겨진 구독 정보를 전달한다.
 *
 * @param result 이전 처리 결과
 * @param moved 넘겨받은 계정에 복사된 구독 정보. {@code APPLIED}가 아니거나 옮길 구독이 없었으면 {@code null}
 */
public record SubscriptionTransferResult(
    SubscriptionUpdateResult result, UserSubscriptionSnapshot moved) {

  /**
   * 사용자를 찾지 못한 결과를 만든다.
   *
   * @return 사용자 없음 결과
   */
  public static SubscriptionTransferResult userNotFound() {
    return new SubscriptionTransferResult(SubscriptionUpdateResult.USER_NOT_FOUND, null);
  }

  /**
   * 이미 반영한 이벤트보다 오래된 이벤트라 무시한 결과를 만든다.
   *
   * @return 오래된 이벤트 결과
   */
  public static SubscriptionTransferResult stale() {
    return new SubscriptionTransferResult(SubscriptionUpdateResult.STALE_EVENT, null);
  }

  /**
   * 이전을 반영한 결과를 만든다.
   *
   * @param moved 넘겨받은 계정에 복사된 구독 정보. 옮길 구독이 없었으면 {@code null}
   * @return 반영 결과
   */
  public static SubscriptionTransferResult applied(UserSubscriptionSnapshot moved) {
    return new SubscriptionTransferResult(SubscriptionUpdateResult.APPLIED, moved);
  }
}
