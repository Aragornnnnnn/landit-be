// Push Queue 메시지 유형별 선택 payload를 정의한다.

package com.landit.landitbe.feature.notification.messaging;

import java.util.List;
import java.util.UUID;

/**
 * Push Queue 메시지 유형별 선택 payload를 정의한다.
 *
 * @param pushDeliveryId Receipt를 확인할 Push Delivery ID
 * @param receiptAttempt Receipt 확인 시도 횟수
 * @param mailboxLetterId 답장 편지 ID
 * @param userProfileIds 답장 수신 사용자 ID 목록
 * @param replyTitle 답장 제목
 * @param campaignId 관리자 푸시 캠페인 ID
 * @param adminId 테스트 대상 관리자 ID
 * @param requestKey 테스트 멱등성 키
 */
public record PushQueuePayload(
    Long pushDeliveryId,
    Integer receiptAttempt,
    Long mailboxLetterId,
    List<Long> userProfileIds,
    String replyTitle,
    UUID campaignId,
    Long adminId,
    String requestKey) {

  /**
   * 관리자 캠페인 ID가 없는 기존 메시지를 생성한다.
   *
   * @param pushDeliveryId Receipt 대상 ID
   * @param receiptAttempt Receipt 확인 횟수
   * @param mailboxLetterId 답장 편지 ID
   * @param userProfileIds 답장 수신자 ID
   * @param replyTitle 답장 제목
   */
  public PushQueuePayload(
      Long pushDeliveryId,
      Integer receiptAttempt,
      Long mailboxLetterId,
      List<Long> userProfileIds,
      String replyTitle) {
    this(
        pushDeliveryId,
        receiptAttempt,
        mailboxLetterId,
        userProfileIds,
        replyTitle,
        null,
        null,
        null);
  }

  /**
   * 기존 Queue payload 생성 계약을 유지한다.
   *
   * @param pushDeliveryId Receipt를 확인할 Push Delivery ID
   * @param receiptAttempt Receipt 확인 시도 횟수
   */
  public PushQueuePayload(Long pushDeliveryId, Integer receiptAttempt) {
    this(pushDeliveryId, receiptAttempt, null, null, null, null, null, null);
  }

  /**
   * 편지함 답장 알림 payload를 생성한다.
   *
   * @param request 답장과 수신자 정보
   * @return 편지함 답장 알림 payload
   */
  public static PushQueuePayload mailboxReply(MailboxReplyNotificationRequest request) {
    return new PushQueuePayload(
        null,
        null,
        request.letterId(),
        request.userProfileIds(),
        request.replyTitle(),
        null,
        null,
        null);
  }

  /**
   * Receipt 확인 payload를 생성한다.
   *
   * @param pushDeliveryId 확인할 Push Delivery ID
   * @param receiptAttempt Receipt 확인 시도 횟수
   * @return Receipt 확인 payload
   */
  public static PushQueuePayload receipt(Long pushDeliveryId, int receiptAttempt) {
    return new PushQueuePayload(pushDeliveryId, receiptAttempt, null, null, null, null, null, null);
  }
}
