// Push Queue 메시지 검증과 유형별 Service 위임을 검증한다.

package com.landit.landitbe.feature.notification.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.feature.notification.service.PushReceiptService;
import com.landit.landitbe.feature.notification.service.ReviewReminderService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

/** Push Queue 메시지 검증과 유형별 Service 위임을 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushQueueMessageHandlerTest {

  @Mock private ReviewReminderService reviewReminderService;

  @Mock private PushReceiptService pushReceiptService;

  @InjectMocks private PushQueueMessageHandler pushQueueMessageHandler;

  /** IaC Scheduler JSON 계약을 역직렬화하고 예약 시각 기준 복습 날짜를 처리한다. */
  @Test
  void handlesSchedulerJsonContract() throws Exception {
    PushQueueMessage message =
        JsonMapper.builder()
            .build()
            .readValue(
                """
                {
                  "version": 1,
                  "messageId": "scheduler-execution-id",
                  "messageType": "REVIEW_REMINDER_BATCH",
                  "occurredAt": "2026-07-23T15:00:00Z",
                  "payload": {}
                }
                """,
                PushQueueMessage.class);

    pushQueueMessageHandler.handle(message);

    verify(reviewReminderService).send(LocalDate.of(2026, 7, 24));
  }

  /** Receipt 확인 메시지의 발송 이력 ID와 시도 횟수를 Service에 전달한다. */
  @Test
  void handlesPushReceiptCheck() {
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "receipt-message-id",
            "PUSH_RECEIPT_CHECK",
            Instant.parse("2026-07-24T11:15:00Z"),
            new PushQueuePayload(10L, 2));

    pushQueueMessageHandler.handle(message);

    verify(pushReceiptService).check(10L, 2);
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
            new PushQueuePayload(10L, 4));

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
            "REVIEW_REMINDER_BATCH",
            Instant.parse("2026-07-24T11:00:00Z"),
            new PushQueuePayload(null, null));

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
            "REVIEW_REMINDER_BATCH",
            Instant.parse("2026-07-24T11:00:00Z"),
            new PushQueuePayload(null, null));
    PushQueueMessage unsupportedType =
        new PushQueueMessage(
            1,
            "unsupported-type",
            "UNKNOWN",
            Instant.parse("2026-07-24T11:00:00Z"),
            new PushQueuePayload(null, null));

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
            new PushQueuePayload(null, 0));

    assertThatThrownBy(() -> pushQueueMessageHandler.handle(message))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
