// 푸시 알림 발송과 Receipt 조회를 외부 제공자와 분리하는 Port다.

package com.landit.landitbe.feature.notification.client;

import java.util.List;

/** 푸시 알림 발송과 Receipt 조회를 외부 제공자와 분리하는 Port다. */
public interface NotificationSender {

  /**
   * 같은 제공자 프로젝트의 푸시 알림을 최대 100건까지 한 번에 전달한다.
   *
   * @param messages 발송할 푸시 메시지 목록
   * @return 요청 순서와 같은 순서의 Push Ticket 결과
   */
  List<PushTicketResult> send(List<PushMessage> messages);

  /**
   * Push Ticket의 배달 Receipt를 조회한다.
   *
   * @param ticketId 외부 제공자가 발급한 Ticket ID
   * @return Receipt 조회 결과
   */
  PushReceiptResult getReceipt(String ticketId);

  /**
   * 여러 Ticket의 Receipt를 입력 순서대로 조회한다.
   *
   * @param ticketIds 최대 100개 Ticket ID
   * @return 입력 순서의 확인 결과
   */
  default java.util.List<PushReceiptResult> getReceipts(java.util.List<String> ticketIds) {
    return ticketIds.stream().map(this::getReceipt).toList();
  }
}
