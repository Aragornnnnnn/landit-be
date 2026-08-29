// Push Queue 메시지 검증과 유형별 Service 위임을 검증한다.

package com.landit.landitbe.feature.notification.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.feature.notification.service.PushReceiptService;
import com.landit.landitbe.feature.notification.service.ScheduledNotificationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Push Queue 메시지 검증과 유형별 Service 위임을 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushQueueMessageHandlerTest {

  @Mock private PushReceiptService pushReceiptService;

  @Mock private ScheduledNotificationService scheduledNotificationService;

  @InjectMocks private PushQueueMessageHandler pushQueueMessageHandler;

  /** Receipt 확인 메시지의 발송 이력 ID와 시도 횟수를 Service에 전달한다. */
  @Test
  void handlesPushReceiptCheck() {
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "receipt-message-id",
            "PUSH_RECEIPT_CHECK",
            Instant.parse("2026-07-24T11:15:00Z"),
            PushQueuePayload.receipt(10L, 2));

    pushQueueMessageHandler.handle(message);

    verify(pushReceiptService).check(10L, 2);
  }

  /** EventBridge Scheduler 배치 메시지는 예정 시각을 기준으로 대상 계산 Service에 위임한다. */
  @Test
  void handlesScheduledNotificationBatch() {
    Instant occurredAt = Instant.parse("2026-07-26T11:00:00Z");
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "scheduler-execution",
            "SCHEDULED_NOTIFICATION_BATCH",
            occurredAt,
            new PushQueuePayload(null, null));

    pushQueueMessageHandler.handle(message);

    verify(scheduledNotificationService)
        .process(org.mockito.ArgumentMatchers.eq(occurredAt), any());
  }

  /** Receipt 확인 횟수 정책은 Handler가 아닌 Receipt Service가 판단한다. */
  @Test
  void delegatesPositiveReceiptAttemptToReceiptService() {
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "receipt-message-id",
            "PUSH_RECEIPT_CHECK",
            Instant.parse("2026-07-24T11:15:00Z"),
            PushQueuePayload.receipt(10L, 4));

    pushQueueMessageHandler.handle(message);

    verify(pushReceiptService).check(10L, 4);
  }

  /** 지원하지 않는 version은 실패시켜 SQS 재시도와 DLQ 이동 대상으로 남긴다. */
  @Test
  void rejectsUnsupportedVersion() {
    PushQueueMessage message =
        new PushQueueMessage(
            2,
            "unsupported-version",
            "PUSH_RECEIPT_CHECK",
            Instant.parse("2026-07-24T11:00:00Z"),
            PushQueuePayload.receipt(1L, 1));

    assertThatThrownBy(() -> pushQueueMessageHandler.handle(message))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** 비어 있는 messageId와 지원하지 않는 messageType을 거부한다. */
  @Test
  void rejectsInvalidMessageIdentityAndType() {
    PushQueueMessage blankMessageId =
        new PushQueueMessage(
            1,
            " ",
            "PUSH_RECEIPT_CHECK",
            Instant.parse("2026-07-24T11:00:00Z"),
            PushQueuePayload.receipt(1L, 1));
    PushQueueMessage unsupportedType =
        new PushQueueMessage(
            1,
            "unsupported-type",
            "UNKNOWN",
            Instant.parse("2026-07-24T11:00:00Z"),
            PushQueuePayload.receipt(1L, 1));

    assertThatThrownBy(() -> pushQueueMessageHandler.handle(blankMessageId))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> pushQueueMessageHandler.handle(unsupportedType))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Receipt 확인에 필요한 발송 이력 ID와 시도 횟수가 없으면 메시지를 거부한다. */
  @Test
  void rejectsInvalidReceiptPayload() {
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "invalid-receipt",
            "PUSH_RECEIPT_CHECK",
            Instant.parse("2026-07-24T11:15:00Z"),
            PushQueuePayload.receipt(null, 0));

    assertThatThrownBy(() -> pushQueueMessageHandler.handle(message))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
