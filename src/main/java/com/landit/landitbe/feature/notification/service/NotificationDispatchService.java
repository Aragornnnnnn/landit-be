// 한 사용자의 발송 가능한 설치에 일반 푸시를 멱등 발송한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 한 사용자의 발송 가능한 설치에 일반 푸시를 멱등 발송한다. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class NotificationDispatchService {

  private static final int EXPO_BATCH_SIZE = 100;
  private static final String EXPO_REQUEST_UNCONFIRMED = "EXPO_REQUEST_UNCONFIRMED";
  private static final String EXPO_TICKET_RESULT_MISMATCH = "EXPO_TICKET_RESULT_MISMATCH";

  private final PushDeviceService pushDeviceService;
  private final PushDeliveryService pushDeliveryService;
  private final NotificationSender notificationSender;
  private final PushQueuePublisher pushQueuePublisher;

  /**
   * 지정한 사용자의 발송 가능한 모든 설치에 같은 이벤트 알림을 보낸다.
   *
   * @param command 이벤트 식별자, 대상 사용자와 표시 내용
   */
  public void send(SendPushNotificationCommand command) {
    scheduleAcceptedDeliveryReceipts(command.eventId());
    List<PreparedPushDelivery> deliveries = new ArrayList<>(EXPO_BATCH_SIZE);
    RetryablePushNotificationException firstFailure = null;
    for (Long pushDeviceId : pushDeviceService.findSendableDeviceIds(command.userProfileId())) {
      pushDeliveryService.prepare(prepareCommand(command, pushDeviceId)).ifPresent(deliveries::add);
      if (deliveries.size() == EXPO_BATCH_SIZE) {
        firstFailure = retainFirstFailure(firstFailure, sendPreparedDeliveries(deliveries));
        deliveries.clear();
      }
    }
    if (!deliveries.isEmpty()) {
      firstFailure = retainFirstFailure(firstFailure, sendPreparedDeliveries(deliveries));
    }
    if (firstFailure != null) {
      throw firstFailure;
    }
  }

  /** 같은 이벤트에서 이미 Ticket을 접수한 이력의 Receipt 확인을 먼저 예약한다. */
  private void scheduleAcceptedDeliveryReceipts(String eventId) {
    pushDeliveryService
        .findAcceptedDeliveryIds(deduplicationKeyPrefix(eventId))
        .forEach(pushDeliveryId -> pushQueuePublisher.scheduleReceiptCheck(pushDeliveryId, 1));
  }

  /** 선점된 알림 묶음을 Expo에 보내고 요청 순서대로 Ticket 결과와 Receipt 예약을 기록한다. */
  private RetryablePushNotificationException sendPreparedDeliveries(
      List<PreparedPushDelivery> deliveries) {
    List<PushTicketResult> results;
    try {
      results =
          notificationSender.send(
              deliveries.stream().map(PreparedPushDelivery::toPushMessage).toList());
    } catch (RetryablePushNotificationException exception) {
      deliveries.forEach(delivery -> pushDeliveryService.markRetryable(delivery.pushDeliveryId()));
      return exception;
    } catch (PushNotificationException exception) {
      markDeliveriesFailed(deliveries, EXPO_REQUEST_UNCONFIRMED);
      return null;
    }
    if (results.size() != deliveries.size()) {
      markDeliveriesFailed(deliveries, EXPO_TICKET_RESULT_MISMATCH);
      return null;
    }
    for (int index = 0; index < deliveries.size(); index++) {
      recordTicketResult(deliveries.get(index), results.get(index));
    }
    return null;
  }

  /** 자동 재발송하면 안 되는 Expo 요청 실패를 발송 이력에 종료 상태로 기록한다. */
  private void markDeliveriesFailed(List<PreparedPushDelivery> deliveries, String errorCode) {
    deliveries.forEach(
        delivery ->
            pushDeliveryService.recordTicketResult(
                delivery.pushDeliveryId(), PushTicketResult.failed(errorCode)));
  }

  /** Ticket 결과를 발송 이력에 기록하고 접수된 알림의 Receipt 확인을 예약한다. */
  private void recordTicketResult(PreparedPushDelivery delivery, PushTicketResult result) {
    pushDeliveryService.recordTicketResult(delivery.pushDeliveryId(), result);
    if (result.accepted()) {
      pushQueuePublisher.scheduleReceiptCheck(delivery.pushDeliveryId(), 1);
    }
  }

  /** 먼저 발생한 실패를 유지해 모든 발송 대상 처리 뒤 SQS 재시도를 유도한다. */
  private RetryablePushNotificationException retainFirstFailure(
      RetryablePushNotificationException firstFailure, RetryablePushNotificationException failure) {
    return firstFailure == null ? failure : firstFailure;
  }

  /** 이벤트·설치 조합의 발송 이력 선점 명령을 생성한다. */
  private PreparePushDeliveryCommand prepareCommand(
      SendPushNotificationCommand command, Long pushDeviceId) {
    return new PreparePushDeliveryCommand(
        command.userProfileId(),
        pushDeviceId,
        command.notificationType(),
        deduplicationKeyPrefix(command.eventId()) + pushDeviceId,
        command.title(),
        command.body(),
        command.deepLink());
  }

  /** 동일 이벤트 발송 이력을 조회할 중복 방지 키 접두어를 만든다. */
  private String deduplicationKeyPrefix(String eventId) {
    return "push:" + eventId + ":";
  }
}
