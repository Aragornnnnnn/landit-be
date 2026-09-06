// Expo Receipt 완료·실패·재확인 흐름을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Expo Receipt 완료·실패·재확인 흐름을 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushReceiptServiceTest {

  private static final long PUSH_DELIVERY_ID = 10L;

  @Mock private PushDeliveryService pushDeliveryService;

  @Mock private NotificationSender notificationSender;

  @Mock private PushQueuePublisher pushQueuePublisher;

  private PushReceiptService pushReceiptService;

  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    pushReceiptService =
        new PushReceiptService(
            pushDeliveryService, notificationSender, pushQueuePublisher, meterRegistry);
  }

  /** Receipt가 준비되지 않았고 시도 횟수가 남으면 다음 확인을 예약한다. */
  @Test
  void schedulesNextCheckWhenReceiptIsNotReady() {
    PushReceiptTarget target = new PushReceiptTarget(PUSH_DELIVERY_ID, "ticket-1");
    when(pushDeliveryService.findReceiptTarget(PUSH_DELIVERY_ID)).thenReturn(Optional.of(target));
    when(notificationSender.getReceipt("ticket-1")).thenReturn(PushReceiptResult.notReady());

    pushReceiptService.check(PUSH_DELIVERY_ID, 1);

    verify(pushQueuePublisher).scheduleReceiptCheck(PUSH_DELIVERY_ID, 2);
    verify(pushDeliveryService, never())
        .recordReceiptResult(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    assertThat(
            meterRegistry
                .find("landit.notification.delivery")
                .tag("stage", "receipt")
                .tag("outcome", "not_ready")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  /** 세 번째 확인에도 Receipt가 없으면 발송 실패로 종료한다. */
  @Test
  void failsWhenReceiptIsStillMissingAfterThirdCheck() {
    PushReceiptTarget target = new PushReceiptTarget(PUSH_DELIVERY_ID, "ticket-1");
    when(pushDeliveryService.findReceiptTarget(PUSH_DELIVERY_ID)).thenReturn(Optional.of(target));
    when(notificationSender.getReceipt("ticket-1")).thenReturn(PushReceiptResult.notReady());

    pushReceiptService.check(PUSH_DELIVERY_ID, 3);

    verify(pushDeliveryService)
        .recordReceiptResult(PUSH_DELIVERY_ID, PushReceiptResult.failed("ReceiptNotAvailable"));
    verify(pushQueuePublisher, never())
        .scheduleReceiptCheck(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
  }

  /** 준비된 Receipt 결과를 발송 이력에 기록한다. */
  @Test
  void recordsReadyReceipt() {
    PushReceiptTarget target = new PushReceiptTarget(PUSH_DELIVERY_ID, "ticket-1");
    PushReceiptResult delivered = PushReceiptResult.delivered();
    when(pushDeliveryService.findReceiptTarget(PUSH_DELIVERY_ID)).thenReturn(Optional.of(target));
    when(notificationSender.getReceipt("ticket-1")).thenReturn(delivered);

    pushReceiptService.check(PUSH_DELIVERY_ID, 1);

    verify(pushDeliveryService).recordReceiptResult(PUSH_DELIVERY_ID, delivered);
    verify(pushQueuePublisher, never())
        .scheduleReceiptCheck(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    assertThat(
            meterRegistry
                .find("landit.notification.delivery")
                .tag("stage", "receipt")
                .tag("outcome", "delivered")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  /** 이미 종료된 발송 이력은 Expo Receipt를 다시 조회하지 않는다. */
  @Test
  void skipsDeliveryThatIsNoLongerWaitingForReceipt() {
    when(pushDeliveryService.findReceiptTarget(PUSH_DELIVERY_ID)).thenReturn(Optional.empty());

    pushReceiptService.check(PUSH_DELIVERY_ID, 1);

    verify(notificationSender, never()).getReceipt(org.mockito.ArgumentMatchers.anyString());
  }
}
