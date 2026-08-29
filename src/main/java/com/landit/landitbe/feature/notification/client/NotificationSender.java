// 푸시 알림 발송과 Receipt 조회를 외부 제공자와 분리하는 Port다.

package com.landit.landitbe.feature.notification.client;

/** 푸시 알림 발송과 Receipt 조회를 외부 제공자와 분리하는 Port다. */
public interface NotificationSender {

  /**
   * 푸시 알림을 외부 제공자에 전달한다.
   *
   * @param message 발송할 푸시 메시지
   * @return Push Ticket 결과
   */
  PushTicketResult send(PushMessage message);

  /**
   * Push Ticket의 배달 Receipt를 조회한다.
   *
   * @param ticketId 외부 제공자가 발급한 Ticket ID
   * @return Receipt 조회 결과
   */
  PushReceiptResult getReceipt(String ticketId);
}
