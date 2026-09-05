// 한 번의 푸시 발송 호출에서 실제 처리한 사용자와 Token 결과를 집계한다.

package com.landit.landitbe.feature.notification.service;

/**
 * 한 번의 푸시 발송 호출에서 실제 처리한 사용자와 Token 결과를 집계한다.
 *
 * @param preparedDeliveries Expo 요청 대상으로 선점한 Token 수
 * @param expoRequestCount Expo API 호출 수
 * @param ticketAccepted Expo가 접수한 Ticket 수
 * @param ticketFailed Expo가 거절한 Ticket 수
 */
public record NotificationDispatchResult(
    int preparedDeliveries, int expoRequestCount, int ticketAccepted, int ticketFailed) {

  /**
   * 발송할 명령이 없는 결과를 반환한다.
   *
   * @return 모든 집계값이 0인 결과
   */
  public static NotificationDispatchResult empty() {
    return new NotificationDispatchResult(0, 0, 0, 0);
  }
}
