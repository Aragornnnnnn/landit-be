// Expo Receipt를 확인하고 미준비 상태를 최대 세 번까지 다시 예약한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushReceiptStatus;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Expo Receipt를 확인하고 미준비 상태를 최대 세 번까지 다시 예약한다. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class PushReceiptService {

  private static final int MAX_ATTEMPTS = 3;
  private static final String RECEIPT_NOT_AVAILABLE = "ReceiptNotAvailable";

  private final PushDeliveryService pushDeliveryService;
  private final NotificationSender notificationSender;
  private final PushQueuePublisher pushQueuePublisher;
  private final MeterRegistry meterRegistry;

  /**
   * Ticket의 Receipt를 조회하고 결과 기록 또는 다음 확인 예약을 처리한다.
   *
   * @param pushDeliveryId Push Delivery ID
   * @param attempt 현재 Receipt 확인 시도 횟수
   */
  public void check(Long pushDeliveryId, int attempt) {
    validateAttempt(attempt);
    pushDeliveryService
        .findReceiptTarget(pushDeliveryId)
        .ifPresent(target -> checkReceipt(target, attempt));
  }

  /**
   * DB에 저장한 관리자 Receipt 회차를 선점해 최대 100건씩 조회한다.
   *
   * @param deliveryIds 발송 이력 ID 목록
   */
  public void checkAdmin(java.util.List<Long> deliveryIds) {
    var targets =
        deliveryIds.stream()
            .map(pushDeliveryService::claimAdminReceipt)
            .flatMap(java.util.Optional::stream)
            .toList();
    if (targets.isEmpty()) {
      return;
    }
    java.util.List<PushReceiptResult> results;
    try {
      results =
          notificationSender.getReceipts(
              targets.stream().map(PushDeliveryService.AdminReceiptTarget::ticketId).toList());
    } catch (com.landit.landitbe.feature.notification.client.PushNotificationException exception) {
      targets.forEach(t -> pushDeliveryService.deferAdminReceipt(t.pushDeliveryId(), t.attempt()));
      return;
    }
    if (results.size() != targets.size()) {
      targets.forEach(t -> pushDeliveryService.deferAdminReceipt(t.pushDeliveryId(), t.attempt()));
      return;
    }
    for (int i = 0; i < targets.size(); i++) {
      var target = targets.get(i);
      if (results.get(i).status() == PushReceiptStatus.NOT_READY) {
        pushDeliveryService.deferAdminReceipt(target.pushDeliveryId(), target.attempt());
      } else {
        pushDeliveryService.recordReceiptResult(target.pushDeliveryId(), results.get(i));
      }
    }
  }

  /** Expo Receipt 결과에 따라 완료 기록 또는 다음 확인 예약을 처리한다. */
  private void checkReceipt(PushReceiptTarget target, int attempt) {
    PushReceiptResult result = notificationSender.getReceipt(target.ticketId());
    meterRegistry
        .counter(
            "landit.notification.delivery",
            "stage",
            "receipt",
            "outcome",
            receiptOutcome(result.status()))
        .increment();
    if (result.status() != PushReceiptStatus.NOT_READY) {
      pushDeliveryService.recordReceiptResult(target.pushDeliveryId(), result);
      return;
    }
    if (attempt < MAX_ATTEMPTS) {
      pushQueuePublisher.scheduleReceiptCheck(target.pushDeliveryId(), attempt + 1);
      return;
    }
    pushDeliveryService.recordReceiptResult(
        target.pushDeliveryId(), PushReceiptResult.failed(RECEIPT_NOT_AVAILABLE));
  }

  /** Receipt 확인 시도 횟수가 지원 범위인지 검증한다. */
  private void validateAttempt(int attempt) {
    if (attempt < 1 || attempt > MAX_ATTEMPTS) {
      throw new IllegalArgumentException("Receipt 확인 시도 횟수가 올바르지 않습니다.");
    }
  }

  /** Receipt 상태를 고정된 저카디널리티 지표 값으로 변환한다. */
  private String receiptOutcome(PushReceiptStatus status) {
    return switch (status) {
      case NOT_READY -> "not_ready";
      case DELIVERED -> "delivered";
      case FAILED -> "failed";
    };
  }
}
