// Push Queue 메시지 유형별 선택 payload를 정의한다.

package com.landit.landitbe.feature.notification.messaging;

import java.util.List;

/**
 * Push Queue 메시지 유형별 선택 payload를 정의한다.
 *
 * @param pushDeliveryId Receipt를 확인할 Push Delivery ID
 * @param receiptAttempt Receipt 확인 시도 횟수
 * @param mailboxLetterId 답장 편지 ID
 * @param userProfileIds 답장 수신 사용자 ID 목록
 * @param replyTitle 답장 제목
 */
public record PushQueuePayload(
    Long pushDeliveryId,
    Integer receiptAttempt,
    Long mailboxLetterId,
    List<Long> userProfileIds,
    String replyTitle) {

  /**
   * 기존 Scheduler의 빈 payload 역직렬화와 테스트 생성을 지원한다.
   *
   * @param pushDeliveryId Receipt를 확인할 Push Delivery ID
   * @param receiptAttempt Receipt 확인 시도 횟수
   */
  public PushQueuePayload(Long pushDeliveryId, Integer receiptAttempt) {
    this(pushDeliveryId, receiptAttempt, null, null, null);
  }

  /**
   * 편지함 답장 알림 payload를 생성한다.
   *
   * @param request 답장과 수신자 정보
   * @return 편지함 답장 알림 payload
   */
  public static PushQueuePayload mailboxReply(MailboxReplyNotificationRequest request) {
    return new PushQueuePayload(
        null, null, request.letterId(), request.userProfileIds(), request.replyTitle());
  }

  /**
   * Receipt 확인 payload를 생성한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param receiptAttempt Receipt 확인 시도 횟수
   * @return Receipt 확인 payload
   */
  public static PushQueuePayload receipt(Long pushDeliveryId, int receiptAttempt) {
    return new PushQueuePayload(pushDeliveryId, receiptAttempt, null, null, null);
  }
}
