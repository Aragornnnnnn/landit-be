// 편지함 답장과 Receipt 확인 메시지를 Push 전용 Queue에 발행하는 Port다.

package com.landit.landitbe.feature.notification.messaging;

import java.util.List;

/** 편지함 답장과 Expo Receipt 확인 메시지를 Push 전용 Queue에 발행하는 Port다. */
public interface PushQueuePublisher {
  /**
   * 편지함 답장 수신자 일괄 알림을 즉시 발행한다.
   *
   * @param request 답장과 수신자 정보
   */
  void publishMailboxReply(MailboxReplyNotificationRequest request);

  /**
   * Expo Receipt 확인 메시지를 지정된 초기 지연으로 발행한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param attempt Receipt 확인 시도 횟수
   */
  void scheduleReceiptCheck(Long pushDeliveryId, int attempt);

  /**
   * Receipt 확인 메시지를 묶음으로 발행한다. 부분 실패도 호출자에게 전파한다.
   *
   * @param pushDeliveryIds 확인할 발송 ID 목록
   * @param attempt Receipt 확인 시도 횟수
   */
  default void scheduleReceiptChecks(List<Long> pushDeliveryIds, int attempt) {
    pushDeliveryIds.forEach(id -> scheduleReceiptCheck(id, attempt));
  }
}
