// 복습 대상의 발송 가능한 설치에 리마인더를 멱등 발송하는 흐름을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.service.ReviewItemService;
import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushMessage;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 복습 대상의 발송 가능한 설치에 리마인더를 멱등 발송하는 흐름을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ReviewReminderServiceTest {

  private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 7, 24);
  private static final PreparedPushDelivery PREPARED_DELIVERY =
      new PreparedPushDelivery(
          10L, "ExponentPushToken[review-token]", "복습할 시간이에요", "오늘의 표현을 다시 볼까요?", "/expressions");

  @Mock private ReviewItemService reviewItemService;

  @Mock private PushDeviceService pushDeviceService;

  @Mock private PushDeliveryService pushDeliveryService;

  @Mock private NotificationSender notificationSender;

  @Mock private PushQueuePublisher pushQueuePublisher;

  @InjectMocks private ReviewReminderService reviewReminderService;

  /** 각 테스트에서 사용할 복습 대상 사용자와 설치를 준비한다. */
  @BeforeEach
  void setUpReminderTarget() {
    when(reviewItemService.findReminderTargetUserIds(REVIEW_DATE)).thenReturn(List.of(1L));
    lenient().when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of(2L));
  }

  /** 복습 대상 사용자의 발송 가능한 설치별로 Ticket을 받고 Receipt 확인을 예약한다. */
  @Test
  void sendsReviewReminderAndSchedulesReceiptCheck() {
    when(pushDeliveryService.prepare(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(PREPARED_DELIVERY.toPushMessage()))
        .thenReturn(PushTicketResult.accepted("ticket-1"));

    reviewReminderService.send(REVIEW_DATE);

    verify(pushDeliveryService).recordTicketResult(10L, PushTicketResult.accepted("ticket-1"));
    verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
  }

  /** 이미 선점된 발송은 Expo를 다시 호출하지 않는다. */
  @Test
  void skipsAlreadyPreparedReviewReminder() {
    when(pushDeliveryService.prepare(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.empty());

    reviewReminderService.send(REVIEW_DATE);

    verify(notificationSender, never()).send(org.mockito.ArgumentMatchers.any(PushMessage.class));
    verify(pushQueuePublisher, never()).scheduleReceiptCheck(10L, 1);
  }

  /** Ticket 접수 이력은 Expo 재발송 없이 Receipt 확인을 다시 예약한다. */
  @Test
  void reschedulesReceiptCheckForAcceptedDeliveryWithoutResendingExpo() {
    when(reviewItemService.findReminderTargetUserIds(REVIEW_DATE)).thenReturn(List.of());
    when(pushDeliveryService.findAcceptedDeliveryIds("review-reminder:" + REVIEW_DATE + ":"))
        .thenReturn(List.of(10L));

    reviewReminderService.send(REVIEW_DATE);

    verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
    verify(pushDeliveryService, never()).prepare(org.mockito.ArgumentMatchers.any());
    verify(notificationSender, never()).send(org.mockito.ArgumentMatchers.any(PushMessage.class));
  }

  /** Ticket 접수 이력의 Receipt 예약을 현재 발송 대상 선점보다 먼저 처리한다. */
  @Test
  void schedulesAcceptedReceiptsBeforePreparingCurrentTargets() {
    when(pushDeliveryService.findAcceptedDeliveryIds("review-reminder:" + REVIEW_DATE + ":"))
        .thenReturn(List.of(10L));
    when(pushDeliveryService.prepare(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.empty());

    reviewReminderService.send(REVIEW_DATE);

    InOrder inOrder = org.mockito.Mockito.inOrder(pushQueuePublisher, pushDeliveryService);
    inOrder.verify(pushQueuePublisher).scheduleReceiptCheck(10L, 1);
    inOrder.verify(pushDeliveryService).prepare(org.mockito.ArgumentMatchers.any());
  }

  /** Ticket 거부 결과는 기록하되 Receipt 확인을 예약하지 않는다. */
  @Test
  void recordsRejectedTicketWithoutReceiptCheck() {
    PushTicketResult rejected = PushTicketResult.failed("DeviceNotRegistered");
    when(pushDeliveryService.prepare(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(PREPARED_DELIVERY.toPushMessage())).thenReturn(rejected);

    reviewReminderService.send(REVIEW_DATE);

    verify(pushDeliveryService).recordTicketResult(10L, rejected);
    verify(pushQueuePublisher, never()).scheduleReceiptCheck(10L, 1);
  }

  /** 일시적인 Expo 오류는 발송 이력을 재시도 가능하게 표시하고 SQS 재시도 예외를 전파한다. */
  @Test
  void marksDeliveryRetryableAndPropagatesTemporaryFailure() {
    RetryablePushNotificationException failure =
        new RetryablePushNotificationException("Expo Push 요청이 일시적으로 실패했습니다.");
    when(pushDeliveryService.prepare(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(PREPARED_DELIVERY.toPushMessage())).thenThrow(failure);

    assertThatThrownBy(() -> reviewReminderService.send(REVIEW_DATE)).isSameAs(failure);
    verify(pushDeliveryService).markRetryable(10L);
  }

  /** 수신 여부를 알 수 없는 오류는 재시도 표식을 남기지 않고 SQS 재시도로 전파한다. */
  @Test
  void doesNotMarkDeliveryRetryableForAmbiguousPushFailure() {
    PushNotificationException failure = new PushNotificationException("Expo Push 요청 결과를 알 수 없습니다.");
    when(pushDeliveryService.prepare(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.of(PREPARED_DELIVERY));
    when(notificationSender.send(PREPARED_DELIVERY.toPushMessage())).thenThrow(failure);

    assertThatThrownBy(() -> reviewReminderService.send(REVIEW_DATE)).isSameAs(failure);

    verify(pushDeliveryService, never()).markRetryable(10L);
  }

  /** 첫 설치 발송이 실패해도 다음 설치까지 처리한 뒤 최초 오류를 다시 전파한다. */
  @Test
  void continuesSendingRemainingDevicesBeforePropagatingFirstFailure() {
    PreparedPushDelivery secondDelivery =
        new PreparedPushDelivery(
            11L,
            "ExponentPushToken[second-review-token]",
            "복습할 시간이에요",
            "오늘의 표현을 다시 볼까요?",
            "/expressions");
    RetryablePushNotificationException firstFailure =
        new RetryablePushNotificationException("첫 설치의 일시 오류");
    when(pushDeviceService.findSendableDeviceIds(1L)).thenReturn(List.of(2L, 3L));
    when(pushDeliveryService.prepare(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.of(PREPARED_DELIVERY), Optional.of(secondDelivery));
    when(notificationSender.send(PREPARED_DELIVERY.toPushMessage())).thenThrow(firstFailure);
    when(notificationSender.send(secondDelivery.toPushMessage()))
        .thenReturn(PushTicketResult.accepted("ticket-2"));

    assertThatThrownBy(() -> reviewReminderService.send(REVIEW_DATE)).isSameAs(firstFailure);

    verify(pushDeliveryService).markRetryable(10L);
    verify(notificationSender).send(secondDelivery.toPushMessage());
    verify(pushDeliveryService).recordTicketResult(11L, PushTicketResult.accepted("ticket-2"));
    verify(pushQueuePublisher).scheduleReceiptCheck(11L, 1);
  }
}
