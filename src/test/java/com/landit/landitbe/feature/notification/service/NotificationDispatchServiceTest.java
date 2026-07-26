// 한 사용자의 발송 가능한 설치에 일반 푸시를 멱등 발송하는 흐름을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 한 사용자의 발송 가능한 설치에 일반 푸시를 멱등 발송하는 흐름을 검증한다. */
@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

  private static final SendPushNotificationCommand COMMAND =
      new SendPushNotificationCommand(
          "event-1", 1L, NotificationType.TEST_NOTIFICATION, "테스트 알림", "정상적으로 도착했어요.", "/home");
  private static final PreparedPushDelivery PREPARED_DELIVERY =
      new PreparedPushDelivery(
          10L, "ExponentPushToken[test-token]", "테스트 알림", "정상적으로 도착했어요.", "/home");

  @Mock private PushDeviceService pushDeviceService;

  @Mock private PushDeliveryService pushDeliveryService;

  @Mock private NotificationSender notificationSender;

  @Mock private PushQueuePublisher pushQueuePublisher;

  @InjectMocks private NotificationDispatchService notificationDispatchService;

  /** 사용자의 발송 가능한 설치별 Ticket을 기록하고 Receipt 확인을 예약한다. */
  @Test
  void sendsNotificationAndSchedulesReceiptCheck() {
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of(2L));
    when(pushDeliveryService.prepare(any())).thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage())))
        .thenReturn(List.of(PushTicketResult.accepted("ticket-1")));

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .prepare(
            argThat(
                command ->
                    command.userProfileId().equals(1L)
                        && command.pushDeviceId().equals(2L)
                        && command.notificationType() == NotificationType.TEST_NOTIFICATION
                        && command.deduplicationKey().equals("push:event-1:2")));
    verify(pushDeliveryService).recordTicketResult(10L, PushTicketResult.accepted("ticket-1"));
    verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
  }

  /** 같은 이벤트에서 이미 Ticket이 접수된 이력은 Expo 재발송 없이 Receipt만 다시 예약한다. */
  @Test
  void reschedulesAcceptedReceiptWithoutResendingExpo() {
    when(pushDeliveryService.findAcceptedDeliveryIds("push:event-1:")).thenReturn(List.of(10L));
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of());

    notificationDispatchService.send(COMMAND);

    verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
    verify(notificationSender, never()).send(anyList());
  }

  /** 일시적인 Expo 오류는 발송 이력을 재시도 가능하게 표시하고 예외를 전파한다. */
  @Test
  void marksDeliveryRetryableAndPropagatesTemporaryFailure() {
    RetryablePushNotificationException failure =
        new RetryablePushNotificationException("Expo Push 요청이 일시적으로 실패했습니다.");
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of(2L));
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
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of(2L));
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
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of(2L));
    when(pushDeliveryService.prepare(any())).thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage()))).thenReturn(List.of());

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .recordTicketResult(10L, PushTicketResult.failed("EXPO_TICKET_RESULT_MISMATCH"));
    verify(pushDeliveryService, never()).markRetryable(10L);
  }

  /** 발송 가능한 설치가 없으면 Expo를 호출하지 않는다. */
  @Test
  void skipsUserWithoutSendableDevice() {
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of());

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService, never()).prepare(any());
    verify(notificationSender, never()).send(anyList());
  }

  /** 100건을 초과하는 설치는 Expo 요청 최대 크기에 맞춰 나누어 전송한다. */
  @Test
  void splitsPreparedDeliveriesAtExpoBatchLimit() {
    List<Long> pushDeviceIds = LongStream.rangeClosed(1, 101).boxed().toList();
    AtomicLong deliveryId = new AtomicLong(1L);
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(pushDeviceIds);
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
}
