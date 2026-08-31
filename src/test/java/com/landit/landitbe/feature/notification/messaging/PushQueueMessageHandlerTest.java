// Push Queue 메시지 검증과 유형별 Service 위임을 검증한다.

package com.landit.landitbe.feature.notification.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.service.PushReceiptService;
import com.landit.landitbe.feature.notification.service.SendPushNotificationCommand;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

/** Push Queue 메시지 검증과 유형별 Service 위임을 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushQueueMessageHandlerTest {

  @Mock private NotificationDispatchService notificationDispatchService;

  @Mock private PushReceiptService pushReceiptService;

  @InjectMocks private PushQueueMessageHandler pushQueueMessageHandler;

  /** 사용자별 푸시 발송 JSON 계약을 역직렬화하고 일반 발송 Service에 전달한다. */
  @Test
  void handlesPushSendJsonContract() throws Exception {
    PushQueueMessage message =
        JsonMapper.builder()
            .build()
            .readValue(
                """
                {
                  "version": 1,
                  "messageId": "event-id",
                  "messageType": "PUSH_SEND",
                  "occurredAt": "2026-07-23T15:00:00Z",
                  "payload": {
                    "userProfileId": 1,
                    "notificationType": "TEST_NOTIFICATION",
                    "title": "테스트 알림",
                    "body": "정상적으로 도착했어요.",
                    "deepLink": "/home"
                  }
                }
                """,
                PushQueueMessage.class);

    pushQueueMessageHandler.handle(message);

    verify(notificationDispatchService)
        .send(
            new SendPushNotificationCommand(
                "event-id",
                1L,
                NotificationType.TEST_NOTIFICATION,
                "테스트 알림",
                "정상적으로 도착했어요.",
                "/home"));
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
            PushQueuePayload.receipt(10L, 2));

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
            "PUSH_SEND",
            Instant.parse("2026-07-24T11:00:00Z"),
            PushQueuePayload.notification(
                new PushNotificationRequest(
                    "unsupported-version",
                    1L,
                    NotificationType.TEST_NOTIFICATION,
                    "제목",
                    "본문",
                    "/home",
                    Instant.parse("2026-07-24T11:00:00Z"))));

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
            "PUSH_SEND",
            Instant.parse("2026-07-24T11:00:00Z"),
            PushQueuePayload.notification(
                new PushNotificationRequest(
                    "blank-message-id",
                    1L,
                    NotificationType.TEST_NOTIFICATION,
                    "제목",
                    "본문",
                    "/home",
                    Instant.parse("2026-07-24T11:00:00Z"))));
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

  /** 사용자 ID나 표시 내용이 없는 푸시 발송 payload를 거부한다. */
  @Test
  void rejectsInvalidPushSendPayload() {
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "invalid-push-send",
            "PUSH_SEND",
            Instant.parse("2026-07-24T11:15:00Z"),
            new PushQueuePayload(
                null, NotificationType.TEST_NOTIFICATION, "제목", "본문", "/home", null, null));

    assertThatThrownBy(() -> pushQueueMessageHandler.handle(message))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
