// 여러 사용자의 발송 가능한 Token에 일반 푸시를 멱등 발송하는 흐름을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushMessage;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 여러 사용자의 발송 가능한 Token에 일반 푸시를 멱등 발송하는 흐름을 검증한다. */
@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

  private static final SendPushNotificationCommand COMMAND =
      new SendPushNotificationCommand(
          "event-1", 1L, NotificationType.TEST_NOTIFICATION, "테스트 알림", "정상적으로 도착했어요.", "/home");
  private static final PreparedPushDelivery PREPARED_DELIVERY =
      new PreparedPushDelivery(
          10L, "ExponentPushToken[test-token]", "테스트 알림", "정상적으로 도착했어요.", "/home");

  @Mock private UserPushTokenDeliveryService userPushTokenDeliveryService;

  @Mock private PushDeliveryService pushDeliveryService;

  @Mock private NotificationSender notificationSender;

  @Mock private PushQueuePublisher pushQueuePublisher;

  @InjectMocks private NotificationDispatchService notificationDispatchService;

  /** 사용자의 발송 가능한 Token별 Ticket을 기록하고 Receipt 확인을 예약한다. */
  @Test
  void sendsNotificationAndSchedulesReceiptCheck() {
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepare(any())).thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage())))
        .thenReturn(List.of(PushTicketResult.accepted("ticket-1")));

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .prepare(
            argThat(
                command ->
                    command.userProfileId().equals(1L)
                        && command.userPushTokenId().equals(2L)
                        && command.notificationType() == NotificationType.TEST_NOTIFICATION
                        && command.deduplicationKey().equals("push:event-1:2")));
    verify(pushDeliveryService).recordTicketResult(10L, PushTicketResult.accepted("ticket-1"));
    verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
  }

  /** 같은 사용자의 과거 알림은 제외하고 현재 이벤트의 접수 Ticket만 Receipt를 다시 예약한다. */
  @Test
  void reschedulesOnlyCurrentEventAcceptedReceiptWithoutResendingExpo() {
    when(pushDeliveryService.findAcceptedDeliveryIds("push:event-1:")).thenReturn(List.of(10L));
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of());

    notificationDispatchService.send(COMMAND);

    verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
    verify(pushQueuePublisher, never()).scheduleReceiptCheck(11L, 1);
    verify(notificationSender, never()).send(anyList());
  }

  /** 일시적인 Expo 오류는 발송 이력을 재시도 가능하게 표시하고 예외를 전파한다. */
  @Test
  void marksDeliveryRetryableAndPropagatesTemporaryFailure() {
    RetryablePushNotificationException failure =
        new RetryablePushNotificationException("Expo Push 요청이 일시적으로 실패했습니다.");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepare(any())).thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage()))).thenThrow(failure);

    assertThatThrownBy(() -> notificationDispatchService.send(COMMAND)).isSameAs(failure);

    verify(pushDeliveryService).markRetryable(10L);
  }

  /** Expo 수신 여부를 확정할 수 없는 오류는 재전달하지 않고 발송 이력을 종료한다. */
  @Test
  void marksDeliveryFailedWithoutRetryingUnconfirmedExpoFailure() {
    PushNotificationException failure =
        new PushNotificationException("Expo Push 응답 형식이 올바르지 않습니다.");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepare(any())).thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage()))).thenThrow(failure);

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .recordTicketResult(10L, PushTicketResult.failed("EXPO_REQUEST_UNCONFIRMED"));
    verify(pushDeliveryService, never()).markRetryable(10L);
  }

  /** Expo Ticket 결과 수가 요청 수와 다르면 발송 이력을 종료하고 메시지를 확인 처리한다. */
  @Test
  void marksDeliveryFailedWithoutRetryingTicketResultCountMismatch() {
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepare(any())).thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage()))).thenReturn(List.of());

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .recordTicketResult(10L, PushTicketResult.failed("EXPO_TICKET_RESULT_MISMATCH"));
    verify(pushDeliveryService, never()).markRetryable(10L);
  }

  /** 발송 가능한 Token이 없으면 Expo를 호출하지 않는다. */
  @Test
  void skipsUserWithoutSendableDevice() {
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of());

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService, never()).prepare(any());
    verify(notificationSender, never()).send(anyList());
  }

  /** 100건을 초과하는 Token은 Expo 요청 최대 크기에 맞춰 나누어 전송한다. */
  @Test
  void splitsPreparedDeliveriesAtExpoBatchLimit() {
    List<Long> userPushTokenIds = LongStream.rangeClosed(1, 101).boxed().toList();
    AtomicLong deliveryId = new AtomicLong(1L);
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, userPushTokenIds));
    when(pushDeliveryService.prepare(any()))
        .thenAnswer(
            invocation ->
                Optional.of(
                    new PreparedPushDelivery(
                        deliveryId.getAndIncrement(),
                        "ExponentPushToken[batch-token]",
                        "테스트 알림",
                        "정상적으로 도착했어요.",
                        "/home")));
    when(notificationSender.send(anyList()))
        .thenAnswer(
            invocation -> {
              List<PushMessage> messages = invocation.<List<PushMessage>>getArgument(0);
              return messages.stream()
                  .map(message -> PushTicketResult.accepted("ticket-" + message.expoPushToken()))
                  .toList();
            });

    notificationDispatchService.send(COMMAND);

    verify(notificationSender, times(2)).send(anyList());
    verify(notificationSender).send(argThat(messages -> messages.size() == 100));
    verify(notificationSender).send(argThat(messages -> messages.size() == 1));
  }

  /** Receipt 예약이 실패해도 같은 Expo 응답의 모든 Ticket 결과를 먼저 기록한다. */
  @Test
  void recordsAllTicketResultsBeforeSchedulingReceipts() {
    PreparedPushDelivery secondDelivery =
        new PreparedPushDelivery(11L, "ExponentPushToken[second-token]", "두 번째 알림", "본문", "/home");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L, 3L)));
    when(pushDeliveryService.prepare(any()))
        .thenReturn(Optional.of(PREPARED_DELIVERY), Optional.of(secondDelivery));
    when(notificationSender.send(anyList()))
        .thenReturn(
            List.of(PushTicketResult.accepted("ticket-1"), PushTicketResult.accepted("ticket-2")));
    PushNotificationException failure = new PushNotificationException("Receipt 예약 실패");
    doThrow(failure).when(pushQueuePublisher).scheduleReceiptCheck(10L, 1);

    assertThatThrownBy(() -> notificationDispatchService.send(COMMAND)).isSameAs(failure);

    verify(pushDeliveryService).recordTicketResult(10L, PushTicketResult.accepted("ticket-1"));
    verify(pushDeliveryService).recordTicketResult(11L, PushTicketResult.accepted("ticket-2"));
  }

  /** 같은 페이지의 여러 사용자 Token을 모아 하나의 Expo 요청으로 전송한다. */
  @Test
  void batchesPreparedDeliveriesAcrossUsers() {
    PreparedPushDelivery secondDelivery =
        new PreparedPushDelivery(11L, "ExponentPushToken[second-token]", "두 번째 알림", "본문", "/home");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L, 2L)))
        .thenReturn(Map.of(1L, List.of(2L), 2L, List.of(3L)));
    when(pushDeliveryService.prepare(any()))
        .thenReturn(Optional.of(PREPARED_DELIVERY), Optional.of(secondDelivery));
    when(notificationSender.send(anyList()))
        .thenReturn(
            List.of(PushTicketResult.accepted("ticket-1"), PushTicketResult.accepted("ticket-2")));
    SendPushNotificationCommand secondCommand =
        new SendPushNotificationCommand(
            "event-2", 2L, NotificationType.TEST_NOTIFICATION, "두 번째 알림", "본문", "/home");

    notificationDispatchService.sendAll(List.of(COMMAND, secondCommand));

    verify(notificationSender)
        .send(List.of(PREPARED_DELIVERY.toPushMessage(), secondDelivery.toPushMessage()));
    verify(userPushTokenDeliveryService, times(1))
        .findSendableTokenIdsByUserProfileIds(List.of(1L, 2L));
    verify(pushDeliveryService).findAcceptedDeliveryIds("push:event-1:");
    verify(pushDeliveryService).findAcceptedDeliveryIds("push:event-2:");
    verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
    verify(pushQueuePublisher).scheduleReceiptCheck(11L, 1);
  }
}
