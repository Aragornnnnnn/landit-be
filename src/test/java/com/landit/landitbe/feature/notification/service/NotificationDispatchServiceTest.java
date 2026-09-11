// 여러 사용자의 발송 가능한 Token에 일반 푸시를 멱등 발송하는 흐름을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  private NotificationDispatchService notificationDispatchService;

  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    notificationDispatchService =
        new NotificationDispatchService(
            userPushTokenDeliveryService,
            pushDeliveryService,
            notificationSender,
            pushQueuePublisher,
            meterRegistry);
  }

  /** 사용자의 발송 가능한 Token별 Ticket을 기록하고 Receipt 확인을 예약한다. */
  @Test
  void sendsNotificationAndSchedulesReceiptCheck() {
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepareAll(any())).thenReturn(List.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage())))
        .thenReturn(List.of(PushTicketResult.accepted("ticket-1")));

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .prepareAll(
            argThat(
                commands -> {
                  PreparePushDeliveryCommand command = commands.getFirst();
                  return command.userProfileId().equals(1L)
                      && command.userPushTokenId().equals(2L)
                      && command.notificationType() == NotificationType.TEST_NOTIFICATION
                      && command.deduplicationKey().equals("push:event-1:2");
                }));
    verify(pushDeliveryService)
        .recordTicketResults(List.of(10L), List.of(PushTicketResult.accepted("ticket-1")));
    verify(pushQueuePublisher).scheduleReceiptChecks(List.of(10L), 1);
    assertThat(
            meterRegistry
                .find("landit.notification.expo.request.duration")
                .tag("outcome", "success")
                .timer()
                .count())
        .isEqualTo(1L);
    assertThat(
            meterRegistry
                .find("landit.notification.delivery")
                .tag("stage", "ticket")
                .tag("outcome", "accepted")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  /** 같은 사용자의 과거 알림은 제외하고 현재 이벤트의 접수 Ticket만 Receipt를 다시 예약한다. */
  @Test
  void reschedulesOnlyCurrentEventAcceptedReceiptWithoutResendingExpo() {
    when(pushDeliveryService.findAcceptedDeliveryIdsForEvents(List.of("push:event-1:")))
        .thenReturn(List.of(10L));
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of());

    notificationDispatchService.send(COMMAND);

    verify(pushQueuePublisher).scheduleReceiptChecks(List.of(10L), 1);
    verify(pushQueuePublisher, never()).scheduleReceiptChecks(List.of(11L), 1);
    verify(notificationSender, never()).send(anyList());
  }

  /** 일시적인 Expo 오류는 발송 이력을 재시도 가능하게 표시하고 예외를 전파한다. */
  @Test
  void marksDeliveryRetryableAndPropagatesTemporaryFailure() {
    RetryablePushNotificationException failure =
        new RetryablePushNotificationException("Expo Push 요청이 일시적으로 실패했습니다.");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepareAll(any())).thenReturn(List.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage()))).thenThrow(failure);

    assertThatThrownBy(() -> notificationDispatchService.send(COMMAND)).isSameAs(failure);

    verify(pushDeliveryService).markRetryable(10L);
    assertThat(
            meterRegistry
                .find("landit.notification.expo.request.duration")
                .tag("outcome", "retryable_failure")
                .timer()
                .count())
        .isEqualTo(1L);
  }

  /** Expo 수신 여부를 확정할 수 없는 오류는 재전달하지 않고 발송 이력을 종료한다. */
  @Test
  void marksDeliveryFailedWithoutRetryingUnconfirmedExpoFailure() {
    PushNotificationException failure =
        new PushNotificationException("Expo Push 응답 형식이 올바르지 않습니다.");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepareAll(any())).thenReturn(List.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage()))).thenThrow(failure);

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .recordTicketResults(
            List.of(10L), List.of(PushTicketResult.failed("EXPO_REQUEST_UNCONFIRMED")));
    verify(pushDeliveryService, never()).markRetryable(10L);
  }

  /** Expo Ticket 결과 수가 요청 수와 다르면 발송 이력을 종료하고 메시지를 확인 처리한다. */
  @Test
  void marksDeliveryFailedWithoutRetryingTicketResultCountMismatch() {
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L)));
    when(pushDeliveryService.prepareAll(any())).thenReturn(List.of(PREPARED_DELIVERY));
    when(notificationSender.send(List.of(PREPARED_DELIVERY.toPushMessage()))).thenReturn(List.of());

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService)
        .recordTicketResults(
            List.of(10L), List.of(PushTicketResult.failed("EXPO_TICKET_RESULT_MISMATCH")));
    verify(pushDeliveryService, never()).markRetryable(10L);
  }

  /** 발송 가능한 Token이 없으면 Expo를 호출하지 않는다. */
  @Test
  void skipsUserWithoutSendableDevice() {
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of());

    notificationDispatchService.send(COMMAND);

    verify(pushDeliveryService, never()).prepareAll(any());
    verify(notificationSender, never()).send(anyList());
  }

  /** 100건을 초과하는 Token은 Expo 요청 최대 크기에 맞춰 나누어 전송한다. */
  @Test
  void splitsPreparedDeliveriesAtExpoBatchLimit() {
    List<Long> userPushTokenIds = LongStream.rangeClosed(1, 101).boxed().toList();
    AtomicLong deliveryId = new AtomicLong(1L);
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, userPushTokenIds));
    when(pushDeliveryService.prepareAll(any()))
        .thenAnswer(
            invocation ->
                invocation.<List<PreparePushDeliveryCommand>>getArgument(0).stream()
                    .map(
                        c ->
                            new PreparedPushDelivery(
                                deliveryId.getAndIncrement(),
                                "ExponentPushToken[batch-token]",
                                "테스트 알림",
                                "정상적으로 도착했어요.",
                                "/home"))
                    .toList());
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

  /** 재처리에서 각 DB 묶음의 발송 대상이 적어도 하나의 Expo 요청으로 합친다. */
  @Test
  void combinesSparsePreparedDeliveriesAcrossDatabaseBatches() {
    List<Long> tokenIds = LongStream.rangeClosed(1, 500).boxed().toList();
    stubSelectivePreparation(tokenIds, id -> (id - 1) % 100 == 0);
    stubAcceptedTickets();

    NotificationDispatchResult result = notificationDispatchService.sendAll(List.of(COMMAND));

    assertThat(result).isEqualTo(new NotificationDispatchResult(5, 1, 5, 0));
    verify(notificationSender).send(argThat(messages -> messages.size() == 5));
    verify(pushQueuePublisher).scheduleReceiptChecks(List.of(1L, 101L, 201L, 301L, 401L), 1);
    verify(pushDeliveryService, times(5)).prepareAll(argThat(commands -> commands.size() == 100));
    verify(pushDeliveryService, never()).prepareAll(argThat(commands -> commands.size() > 100));
  }

  /** DB 묶음 경계를 넘어 합쳐도 전송 순서·최대 크기·마지막 잔여분을 유지한다. */
  @Test
  void fillsExpoBatchAndFlushesRemainderWithoutDroppingPreparedDeliveries() {
    List<Long> tokenIds = LongStream.rangeClosed(1, 300).boxed().toList();
    stubSelectivePreparation(tokenIds, id -> id <= 75 || (id > 100 && id <= 175));
    stubAcceptedTickets();

    NotificationDispatchResult result = notificationDispatchService.sendAll(List.of(COMMAND));

    assertThat(result).isEqualTo(new NotificationDispatchResult(150, 2, 150, 0));
    org.mockito.ArgumentCaptor<List<PushMessage>> captor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(notificationSender, times(2)).send(captor.capture());
    assertThat(captor.getAllValues()).extracting(List::size).containsExactly(100, 50);
    assertThat(
            captor.getAllValues().stream()
                .flatMap(List::stream)
                .map(PushMessage::expoPushToken)
                .toList())
        .containsExactlyElementsOf(
            tokenIds.stream()
                .filter(id -> id <= 75 || (id > 100 && id <= 175))
                .map(id -> "ExponentPushToken[token-" + id + "]")
                .toList());
    verify(pushDeliveryService, never()).prepareAll(argThat(commands -> commands.size() > 100));
  }

  /** Receipt 예약 실패 시 이미 접수한 100건은 유지하고 미전송 잔여분만 복구한다. */
  @Test
  void recoversOnlyUnsentOverflowAfterPublishingFullBatchFails() {
    List<Long> tokenIds = LongStream.rangeClosed(1, 300).boxed().toList();
    stubSelectivePreparation(tokenIds, id -> id <= 75 || (id > 100 && id <= 175));
    stubAcceptedTickets();
    PushNotificationException failure = new PushNotificationException("Receipt 예약 실패");
    doThrow(failure)
        .when(pushQueuePublisher)
        .scheduleReceiptChecks(anyList(), org.mockito.ArgumentMatchers.eq(1));

    assertThatThrownBy(() -> notificationDispatchService.send(COMMAND)).isSameAs(failure);

    org.mockito.ArgumentCaptor<List<PreparePushDeliveryCommand>> captor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(pushDeliveryService, times(2)).prepareAll(captor.capture());
    assertThat(captor.getAllValues()).extracting(List::size).containsExactly(100, 100);
    assertThat(
            captor.getAllValues().stream()
                .flatMap(List::stream)
                .map(PreparePushDeliveryCommand::userPushTokenId)
                .toList())
        .containsExactlyElementsOf(LongStream.rangeClosed(1, 200).boxed().toList());
    for (long id = 126; id <= 175; id++) {
      verify(pushDeliveryService).markRetryable(id);
    }
    verify(pushDeliveryService, times(50)).markRetryable(any());
    verify(notificationSender).send(argThat(messages -> messages.size() == 100));
  }

  /** 다음 DB 선점이 실패하면 이미 버퍼에 담긴 미전송 이력을 재시도 가능하게 복구한다. */
  @Test
  void makesUnsentBufferedDeliveriesRetryableWhenNextPreparationFails() {
    stubSelectivePreparation(LongStream.rangeClosed(1, 200).boxed().toList(), id -> id <= 75);
    RuntimeException failure = new IllegalStateException("다음 묶음 선점 실패");
    doThrow(failure)
        .when(pushDeliveryService)
        .prepareAll(argThat(commands -> commands.getFirst().userPushTokenId() == 101L));

    assertThatThrownBy(() -> notificationDispatchService.send(COMMAND)).isSameAs(failure);

    for (long id = 1; id <= 75; id++) {
      verify(pushDeliveryService).markRetryable(id);
    }
    verify(pushDeliveryService, times(75)).markRetryable(any());
    verify(notificationSender, never()).send(anyList());
  }

  /** 복구 중 오류가 나도 나머지 이력을 복구하고 처음 발생한 예외를 유지한다. */
  @Test
  void retainsPreparationFailureAndAttemptsRemainingRecovery() {
    stubSelectivePreparation(LongStream.rangeClosed(1, 102).boxed().toList(), id -> id <= 2);
    RuntimeException failure = new IllegalStateException("마지막 묶음 선점 실패");
    RuntimeException recoveryFailure = new IllegalStateException("첫 이력 복구 실패");
    doThrow(failure)
        .when(pushDeliveryService)
        .prepareAll(argThat(commands -> commands.size() == 2));
    doThrow(recoveryFailure).when(pushDeliveryService).markRetryable(1L);

    assertThatThrownBy(() -> notificationDispatchService.send(COMMAND))
        .isSameAs(failure)
        .hasSuppressedException(recoveryFailure);

    verify(pushDeliveryService).markRetryable(2L);
    verify(notificationSender, never()).send(anyList());
  }

  /** 전송 버퍼에 한 자리만 남아도 DB 후보 조회가 한 건씩 쪼개지지 않는다. */
  @Test
  void keepsDatabaseBatchesFullWhenExpoBufferHasOneRemainingSlot() {
    stubSelectivePreparation(
        LongStream.rangeClosed(1, 500).boxed().toList(), id -> id <= 99 || id == 500);
    stubAcceptedTickets();

    assertThat(notificationDispatchService.sendAll(List.of(COMMAND)))
        .isEqualTo(new NotificationDispatchResult(100, 1, 100, 0));
    verify(pushDeliveryService, times(5)).prepareAll(argThat(commands -> commands.size() == 100));
    verify(notificationSender).send(argThat(messages -> messages.size() == 100));
  }

  /** 마지막 DB 묶음을 합쳐 100건을 넘겨도 전송 한도를 유지하며 모두 비운다. */
  @Test
  void splitsFinalCombinedRemainderAtExpoLimit() {
    stubSelectivePreparation(LongStream.rangeClosed(1, 199).boxed().toList(), id -> id != 100);
    stubAcceptedTickets();

    assertThat(notificationDispatchService.sendAll(List.of(COMMAND)))
        .isEqualTo(new NotificationDispatchResult(198, 2, 198, 0));
    verify(notificationSender).send(argThat(messages -> messages.size() == 100));
    verify(notificationSender).send(argThat(messages -> messages.size() == 98));
  }

  /** 실제 토큰 ID로 이미 처리된 후보를 제외하는 선점 결과를 구성한다. */
  private void stubSelectivePreparation(
      List<Long> tokenIds, java.util.function.LongPredicate selected) {
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, tokenIds));
    when(pushDeliveryService.prepareAll(anyList()))
        .thenAnswer(
            call ->
                call.<List<PreparePushDeliveryCommand>>getArgument(0).stream()
                    .filter(c -> selected.test(c.userPushTokenId()))
                    .map(
                        c ->
                            new PreparedPushDelivery(
                                c.userPushTokenId(),
                                "ExponentPushToken[token-" + c.userPushTokenId() + "]",
                                c.title(),
                                c.body(),
                                c.deepLink()))
                    .toList());
  }

  private void stubAcceptedTickets() {
    when(notificationSender.send(anyList()))
        .thenAnswer(
            call ->
                call.<List<PushMessage>>getArgument(0).stream()
                    .map(m -> PushTicketResult.accepted("ticket-" + m.expoPushToken()))
                    .toList());
  }

  /** Receipt 예약이 실패해도 같은 Expo 응답의 모든 Ticket 결과를 먼저 기록한다. */
  @Test
  void recordsAllTicketResultsBeforeSchedulingReceipts() {
    PreparedPushDelivery secondDelivery =
        new PreparedPushDelivery(11L, "ExponentPushToken[second-token]", "두 번째 알림", "본문", "/home");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(2L, 3L)));
    when(pushDeliveryService.prepareAll(any()))
        .thenReturn(List.of(PREPARED_DELIVERY, secondDelivery));
    when(notificationSender.send(anyList()))
        .thenReturn(
            List.of(PushTicketResult.accepted("ticket-1"), PushTicketResult.accepted("ticket-2")));
    PushNotificationException failure = new PushNotificationException("Receipt 예약 실패");
    doThrow(failure).when(pushQueuePublisher).scheduleReceiptChecks(List.of(10L, 11L), 1);

    assertThatThrownBy(() -> notificationDispatchService.send(COMMAND)).isSameAs(failure);

    org.mockito.InOrder order =
        org.mockito.Mockito.inOrder(pushDeliveryService, pushQueuePublisher);
    order
        .verify(pushDeliveryService)
        .recordTicketResults(
            List.of(10L, 11L),
            List.of(PushTicketResult.accepted("ticket-1"), PushTicketResult.accepted("ticket-2")));
    order.verify(pushQueuePublisher).scheduleReceiptChecks(List.of(10L, 11L), 1);
  }

  /** 같은 페이지의 여러 사용자 Token을 모아 하나의 Expo 요청으로 전송한다. */
  @Test
  void batchesPreparedDeliveriesAcrossUsers() {
    PreparedPushDelivery secondDelivery =
        new PreparedPushDelivery(11L, "ExponentPushToken[second-token]", "두 번째 알림", "본문", "/home");
    when(userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(List.of(1L, 2L)))
        .thenReturn(Map.of(1L, List.of(2L), 2L, List.of(3L)));
    when(pushDeliveryService.prepareAll(any()))
        .thenReturn(List.of(PREPARED_DELIVERY, secondDelivery));
    when(notificationSender.send(anyList()))
        .thenReturn(
            List.of(PushTicketResult.accepted("ticket-1"), PushTicketResult.accepted("ticket-2")));
    SendPushNotificationCommand secondCommand =
        new SendPushNotificationCommand(
            "event-2", 2L, NotificationType.TEST_NOTIFICATION, "두 번째 알림", "본문", "/home");

    NotificationDispatchResult result =
        notificationDispatchService.sendAll(List.of(COMMAND, secondCommand));

    verify(notificationSender)
        .send(List.of(PREPARED_DELIVERY.toPushMessage(), secondDelivery.toPushMessage()));
    verify(userPushTokenDeliveryService, times(1))
        .findSendableTokenIdsByUserProfileIds(List.of(1L, 2L));
    verify(pushDeliveryService)
        .findAcceptedDeliveryIdsForEvents(List.of("push:event-1:", "push:event-2:"));
    verify(pushQueuePublisher).scheduleReceiptChecks(List.of(10L, 11L), 1);
    assertThat(result).isEqualTo(new NotificationDispatchResult(2, 1, 2, 0));
  }
}
