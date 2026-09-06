// 외부 Push 제공자의 발송 접수 결과를 정의한다.

package com.landit.landitbe.feature.notification.client;

/**
 * 외부 Push 제공자의 발송 접수 결과를 정의한다.
 *
 * @param accepted 외부 제공자 접수 여부
 * @param ticketId Receipt 조회에 사용할 Ticket ID
 * @param errorCode 접수 실패 오류 코드
 */
public record PushTicketResult(boolean accepted, String ticketId, String errorCode) {

  /**
   * 접수된 Push Ticket 결과를 생성한다.
   *
   * @param ticketId Receipt 조회에 사용할 Ticket ID
   * @return 접수 성공 결과
   */
  public static PushTicketResult accepted(String ticketId) {
    return new PushTicketResult(true, ticketId, null);
  }

  /**
   * 거부된 Push Ticket 결과를 생성한다.
   *
   * @param errorCode 접수 실패 오류 코드
   * @return 접수 실패 결과
   */
  public static PushTicketResult failed(String errorCode) {
    return new PushTicketResult(false, null, errorCode);
  }
}
