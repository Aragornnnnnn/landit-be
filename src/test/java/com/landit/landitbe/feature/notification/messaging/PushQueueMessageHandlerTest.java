// Push Queue 메시지 검증과 유형별 Service 위임을 검증한다.

package com.landit.landitbe.feature.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.feature.notification.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.service.PushReceiptService;
import com.landit.landitbe.feature.notification.service.ScheduledNotificationService;
import com.landit.landitbe.feature.notification.service.SendPushNotificationCommand;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Push Queue 메시지 검증과 유형별 Service 위임을 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushQueueMessageHandlerTest {

  @Mock private PushReceiptService pushReceiptService;

  @Mock private ScheduledNotificationService scheduledNotificationService;

  @Mock private NotificationDispatchService notificationDispatchService;

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
        .process(org.mockito.ArgumentMatchers.eq("scheduler-execution"), eq(occurredAt), any());
  }

  @Test
  void handlesMailboxReplyNotificationBatch() {
    Instant occurredAt = Instant.parse("2026-09-02T00:00:00Z");
    String expectedDeepLink =
        "/mailbox/received/10"
            + "?utm_source=push&utm_medium=notification&utm_campaign=mailbox_reply";
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "mailbox-reply:10",
            "MAILBOX_REPLY_NOTIFICATION_BATCH",
            occurredAt,
            PushQueuePayload.mailboxReply(
                new MailboxReplyNotificationRequest(10L, List.of(1L, 2L), "답변 제목", occurredAt)));

    pushQueueMessageHandler.handle(message);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<SendPushNotificationCommand>> commandsCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(notificationDispatchService).sendAll(commandsCaptor.capture());
    assertThat(commandsCaptor.getValue())
        .extracting(
            SendPushNotificationCommand::eventId,
            SendPushNotificationCommand::userProfileId,
            command -> command.notificationType().name(),
            SendPushNotificationCommand::title,
            SendPushNotificationCommand::body,
            SendPushNotificationCommand::deepLink)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                "mailbox-reply:10:1",
                1L,
                "MAILBOX_REPLY",
                "문의에 답변이 도착했어요",
                "답변 제목",
                expectedDeepLink),
            org.assertj.core.groups.Tuple.tuple(
                "mailbox-reply:10:2",
                2L,
                "MAILBOX_REPLY",
                "문의에 답변이 도착했어요",
                "답변 제목",
                expectedDeepLink));
  }

  /** 편지함 답장 배치를 발송하는 동안 SQS visibility를 발송 전후로 연장한다. */
  @Test
  void extendsVisibilityAroundMailboxReplyBatchDispatch() {
    Instant occurredAt = Instant.parse("2026-09-02T00:00:00Z");
    PushQueueMessage message =
        new PushQueueMessage(
            1,
            "mailbox-reply:10",
            "MAILBOX_REPLY_NOTIFICATION_BATCH",
            occurredAt,
            PushQueuePayload.mailboxReply(
                new MailboxReplyNotificationRequest(10L, List.of(1L), "답변 제목", occurredAt)));
    Runnable visibilityExtender = mock(Runnable.class);

    pushQueueMessageHandler.handle(message, visibilityExtender);

    InOrder dispatchOrder = inOrder(visibilityExtender, notificationDispatchService);
    dispatchOrder.verify(visibilityExtender).run();
    dispatchOrder.verify(notificationDispatchService).sendAll(any());
    dispatchOrder.verify(visibilityExtender).run();
  }

  @Test
  void rejectsInvalidMailboxReplyNotificationPayload() {
    Instant occurredAt = Instant.parse("2026-09-02T00:00:00Z");
    List<PushQueuePayload> invalidPayloads =
        List.of(
            new PushQueuePayload(null, null, null, List.of(1L), "답변 제목"),
            new PushQueuePayload(null, null, 10L, null, "답변 제목"),
            new PushQueuePayload(null, null, 10L, List.of(), "답변 제목"),
            new PushQueuePayload(
                null, null, 10L, java.util.Collections.singletonList(null), "답변 제목"),
            new PushQueuePayload(null, null, 10L, List.of(1L), null),
            new PushQueuePayload(null, null, 10L, List.of(1L), " "));

    invalidPayloads.forEach(
        payload -> {
          PushQueueMessage message =
              new PushQueueMessage(
                  1, "mailbox-reply:10", "MAILBOX_REPLY_NOTIFICATION_BATCH", occurredAt, payload);
          assertThatThrownBy(() -> pushQueueMessageHandler.handle(message))
              .isInstanceOf(IllegalArgumentException.class);
        });
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
