// 여러 사용자의 발송 가능한 Token에 일반 푸시를 멱등 발송한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 여러 사용자의 발송 가능한 Token에 일반 푸시를 멱등 발송한다. */
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

  private final UserPushTokenDeliveryService userPushTokenDeliveryService;
  private final PushDeliveryService pushDeliveryService;
  private final NotificationSender notificationSender;
  private final PushQueuePublisher pushQueuePublisher;
  private final MeterRegistry meterRegistry;

  /**
   * 지정한 사용자의 발송 가능한 모든 Token에 같은 이벤트 알림을 보낸다.
   *
   * @param command 이벤트 식별자, 대상 사용자와 표시 내용
   */
  public void send(SendPushNotificationCommand command) {
    sendAll(List.of(command));
  }

  /**
   * 여러 사용자의 발송 가능한 Token을 한 번에 조회해 Expo 요청 제한에 맞춰 발송한다.
   *
   * @param commands 이벤트 식별자, 대상 사용자와 표시 내용 목록
   * @return 실제로 처리한 사용자·Token·Expo Ticket 집계
   */
  public NotificationDispatchResult sendAll(List<SendPushNotificationCommand> commands) {
    if (commands.isEmpty()) {
      return NotificationDispatchResult.empty();
    }
    List<Long> userProfileIds =
        commands.stream().map(SendPushNotificationCommand::userProfileId).distinct().toList();
    Map<Long, List<Long>> userPushTokenIdsByUserProfileId =
        userPushTokenDeliveryService.findSendableTokenIdsByUserProfileIds(userProfileIds);
    List<PreparedPushDelivery> deliveries = new ArrayList<>(EXPO_BATCH_SIZE);
    RetryablePushNotificationException firstFailure = null;
    int preparedDeliveries = 0;
    int expoRequestCount = 0;
    int ticketAccepted = 0;
    int ticketFailed = 0;
    for (SendPushNotificationCommand command : commands) {
      scheduleAcceptedDeliveryReceipts(command.eventId());
      for (Long userPushTokenId :
          userPushTokenIdsByUserProfileId.getOrDefault(command.userProfileId(), List.of())) {
        pushDeliveryService
            .prepare(prepareCommand(command, userPushTokenId))
            .ifPresent(deliveries::add);
        if (deliveries.size() == EXPO_BATCH_SIZE) {
          DispatchBatchResult result = sendPreparedDeliveries(deliveries);
          preparedDeliveries += deliveries.size();
          expoRequestCount++;
          ticketAccepted += result.ticketAccepted();
          ticketFailed += result.ticketFailed();
          firstFailure = retainFirstFailure(firstFailure, result.retryableFailure());
          deliveries.clear();
        }
      }
    }
    if (!deliveries.isEmpty()) {
      DispatchBatchResult result = sendPreparedDeliveries(deliveries);
      preparedDeliveries += deliveries.size();
      expoRequestCount++;
      ticketAccepted += result.ticketAccepted();
      ticketFailed += result.ticketFailed();
      firstFailure = retainFirstFailure(firstFailure, result.retryableFailure());
    }
    if (firstFailure != null) {
      throw firstFailure;
    }
    return new NotificationDispatchResult(
        preparedDeliveries, expoRequestCount, ticketAccepted, ticketFailed);
  }

  /** 같은 발송 이벤트에서 이미 Ticket을 접수한 이력의 Receipt 확인을 다시 예약한다. */
  private void scheduleAcceptedDeliveryReceipts(String eventId) {
    pushDeliveryService
        .findAcceptedDeliveryIds(deduplicationKeyPrefix(eventId))
        .forEach(pushDeliveryId -> pushQueuePublisher.scheduleReceiptCheck(pushDeliveryId, 1));
  }

  /** 선점된 알림 묶음을 Expo에 보내고 요청 순서대로 Ticket 결과와 Receipt 예약을 기록한다. */
  private DispatchBatchResult sendPreparedDeliveries(List<PreparedPushDelivery> deliveries) {
    long startedAt = System.nanoTime();
    List<PushTicketResult> results;
    try {
      results =
          notificationSender.send(
              deliveries.stream().map(PreparedPushDelivery::toPushMessage).toList());
    } catch (RetryablePushNotificationException exception) {
      recordExpoRequestDuration(startedAt, "retryable_failure");
      deliveries.forEach(delivery -> pushDeliveryService.markRetryable(delivery.pushDeliveryId()));
      return new DispatchBatchResult(0, 0, exception);
    } catch (PushNotificationException exception) {
      recordExpoRequestDuration(startedAt, "terminal_failure");
      markDeliveriesFailed(deliveries, EXPO_REQUEST_UNCONFIRMED);
      return new DispatchBatchResult(0, 0, null);
    }
    if (results.size() != deliveries.size()) {
      recordExpoRequestDuration(startedAt, "terminal_failure");
      markDeliveriesFailed(deliveries, EXPO_TICKET_RESULT_MISMATCH);
      return new DispatchBatchResult(0, 0, null);
    }
    recordExpoRequestDuration(startedAt, "success");
    int ticketAccepted = (int) results.stream().filter(PushTicketResult::accepted).count();
    int ticketFailed = results.size() - ticketAccepted;
    for (int index = 0; index < deliveries.size(); index++) {
      pushDeliveryService.recordTicketResult(
          deliveries.get(index).pushDeliveryId(), results.get(index));
    }
    recordTicketCount("accepted", ticketAccepted);
    recordTicketCount("failed", ticketFailed);
    for (int index = 0; index < deliveries.size(); index++) {
      if (results.get(index).accepted()) {
        pushQueuePublisher.scheduleReceiptCheck(deliveries.get(index).pushDeliveryId(), 1);
      }
    }
    return new DispatchBatchResult(ticketAccepted, ticketFailed, null);
  }

  /** Expo 요청 시간을 결과별 Timer에 기록한다. */
  private void recordExpoRequestDuration(long startedAt, String outcome) {
    Timer.builder("landit.notification.expo.request.duration")
        .tag("outcome", outcome)
        .register(meterRegistry)
        .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
  }

  /** Expo Ticket 결과 건수를 기록한다. */
  private void recordTicketCount(String outcome, int count) {
    if (count == 0) {
      return;
    }
    meterRegistry
        .counter("landit.notification.delivery", "stage", "ticket", "outcome", outcome)
        .increment(count);
  }

  /** 자동 재발송하면 안 되는 Expo 요청 실패를 발송 이력에 종료 상태로 기록한다. */
  private void markDeliveriesFailed(List<PreparedPushDelivery> deliveries, String errorCode) {
    deliveries.forEach(
        delivery ->
            pushDeliveryService.recordTicketResult(
                delivery.pushDeliveryId(), PushTicketResult.failed(errorCode)));
  }

  /** 먼저 발생한 실패를 유지해 모든 발송 대상 처리 뒤 SQS 재시도를 유도한다. */
  private RetryablePushNotificationException retainFirstFailure(
      RetryablePushNotificationException firstFailure, RetryablePushNotificationException failure) {
    return firstFailure == null ? failure : firstFailure;
  }

  /** 이벤트·Token 조합의 발송 이력 선점 명령을 생성한다. */
  private PreparePushDeliveryCommand prepareCommand(
      SendPushNotificationCommand command, Long userPushTokenId) {
    return new PreparePushDeliveryCommand(
        command.userProfileId(),
        userPushTokenId,
        command.notificationType(),
        command.contentVariant(),
        deduplicationKeyPrefix(command.eventId()) + userPushTokenId,
        command.title(),
        command.body(),
        command.deepLink());
  }

  /** 동일 이벤트 발송 이력을 조회할 중복 방지 키 접두어를 만든다. */
  private String deduplicationKeyPrefix(String eventId) {
    return "push:" + eventId + ":";
  }

  /** 한 번의 Expo API 호출 결과와 재시도 예외를 함께 전달한다. */
  private record DispatchBatchResult(
      int ticketAccepted, int ticketFailed, RetryablePushNotificationException retryableFailure) {}
}
