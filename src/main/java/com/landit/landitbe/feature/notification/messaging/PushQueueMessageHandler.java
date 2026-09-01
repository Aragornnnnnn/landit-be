// Push Queue 메시지를 검증하고 유형별 알림 Service에 위임한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.service.PushReceiptService;
import com.landit.landitbe.feature.notification.service.ScheduledNotificationService;
import com.landit.landitbe.feature.notification.service.SendPushNotificationCommand;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Push Queue 메시지를 검증하고 유형별 알림 Service에 위임한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class PushQueueMessageHandler {

  private static final int SUPPORTED_VERSION = 1;
  private static final String SCHEDULED_NOTIFICATION_BATCH = "SCHEDULED_NOTIFICATION_BATCH";
  private static final String MAILBOX_REPLY_TITLE = "문의에 답변이 도착했어요";

  private final PushReceiptService pushReceiptService;
  private final ScheduledNotificationService scheduledNotificationService;
  private final NotificationDispatchService notificationDispatchService;

  /**
   * 메시지 공통 계약과 유형별 payload를 검증한 뒤 알림 흐름을 실행한다.
   *
   * @param message Push Queue 메시지
   */
  public void handle(PushQueueMessage message) {
    handle(message, () -> {});
  }

  /**
   * 메시지 공통 계약과 유형별 payload를 검증한 뒤 알림 흐름을 실행한다.
   *
   * @param message Push Queue 메시지
   * @param visibilityExtender 긴 배치 처리 중 SQS visibility를 연장하는 작업
   */
  public void handle(PushQueueMessage message, Runnable visibilityExtender) {
    validateCommon(message);
    switch (message.messageType()) {
      case PushQueueMessage.MAILBOX_REPLY_NOTIFICATION_BATCH ->
          handleMailboxReplyNotificationBatch(message);
      case PushQueueMessage.PUSH_RECEIPT_CHECK -> handleReceiptCheck(message.payload());
      case SCHEDULED_NOTIFICATION_BATCH ->
          scheduledNotificationService.process(message.occurredAt(), visibilityExtender);
      default -> throw new IllegalArgumentException("지원하지 않는 Push 메시지 유형입니다.");
    }
  }

  /** 모든 Push Queue 메시지가 만족해야 하는 공통 계약을 검증한다. */
  private void validateCommon(PushQueueMessage message) {
    if (message == null
        || message.version() != SUPPORTED_VERSION
        || message.messageId() == null
        || message.messageId().isBlank()
        || message.messageType() == null
        || message.occurredAt() == null
        || message.payload() == null) {
      throw new IllegalArgumentException("Push Queue 메시지 계약이 올바르지 않습니다.");
    }
  }

  /** Receipt 확인 payload를 검증하고 Receipt Service에 전달한다. */
  private void handleReceiptCheck(PushQueuePayload payload) {
    if (payload.pushDeliveryId() == null
        || payload.receiptAttempt() == null
        || payload.receiptAttempt() < 1) {
      throw new IllegalArgumentException("Push Receipt payload가 올바르지 않습니다.");
    }
    pushReceiptService.check(payload.pushDeliveryId(), payload.receiptAttempt());
  }

  /** 편지함 답장 payload를 검증하고 사용자별 발송 명령으로 전달한다. */
  private void handleMailboxReplyNotificationBatch(PushQueueMessage message) {
    PushQueuePayload payload = message.payload();
    validateMailboxReplyPayload(payload);
    String deepLink = mailboxReplyDeepLink(payload.mailboxLetterId());
    notificationDispatchService.sendAll(
        payload.userProfileIds().stream()
            .map(
                userProfileId ->
                    new SendPushNotificationCommand(
                        message.messageId() + ":" + userProfileId,
                        userProfileId,
                        NotificationType.MAILBOX_REPLY,
                        MAILBOX_REPLY_TITLE,
                        payload.replyTitle(),
                        deepLink))
            .toList());
  }

  /** 답장 알림 발송에 필요한 편지, 수신자와 제목을 검증한다. */
  private void validateMailboxReplyPayload(PushQueuePayload payload) {
    if (payload.mailboxLetterId() == null
        || payload.userProfileIds() == null
        || payload.userProfileIds().isEmpty()
        || payload.userProfileIds().stream().anyMatch(Objects::isNull)
        || payload.replyTitle() == null
        || payload.replyTitle().isBlank()) {
      throw new IllegalArgumentException("편지함 답장 Push payload가 올바르지 않습니다.");
    }
  }

  /** 편지함 답장 상세 화면의 Push 유입 경로를 만든다. */
  private String mailboxReplyDeepLink(Long mailboxLetterId) {
    return "/mailbox/received/"
        + mailboxLetterId
        + "?utm_source=push&utm_medium=notification&utm_campaign=mailbox_reply";
  }
}
