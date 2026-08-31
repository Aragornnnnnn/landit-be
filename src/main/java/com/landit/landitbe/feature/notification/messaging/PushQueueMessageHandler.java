// Push Queue 메시지를 검증하고 유형별 알림 Service에 위임한다.

package com.landit.landitbe.feature.notification.messaging;

import com.landit.landitbe.feature.notification.service.PushReceiptService;
import com.landit.landitbe.feature.notification.service.ScheduledNotificationService;
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

  private final PushReceiptService pushReceiptService;
  private final ScheduledNotificationService scheduledNotificationService;

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
}
