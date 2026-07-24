// 기준 날짜의 복습 대상 설치에 리마인더를 멱등 발송한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.learning.service.ReviewItemService;
import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 기준 날짜의 복습 대상 설치에 리마인더를 멱등 발송한다. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class ReviewReminderService {

  private static final String TITLE = "복습할 시간이에요";
  private static final String BODY = "오늘의 표현을 다시 볼까요?";
  private static final String DEEP_LINK =
      "/expressions?utm_source=push&utm_medium=notification&utm_campaign=review_reminder";

  private final ReviewItemService reviewItemService;
  private final PushDeviceService pushDeviceService;
  private final PushDeliveryService pushDeliveryService;
  private final NotificationSender notificationSender;
  private final PushQueuePublisher pushQueuePublisher;

  /**
   * 기준 날짜에 복습 문항이 있는 활성 사용자의 발송 가능한 설치에 리마인더를 보낸다.
   *
   * @param reviewDate 복습 기준 날짜
   */
  public void send(LocalDate reviewDate) {
    scheduleAcceptedDeliveryReceipts(reviewDate);
    PushNotificationException firstFailure = null;
    for (Long userProfileId : reviewItemService.findReminderTargetUserIds(reviewDate)) {
      for (Long pushDeviceId : pushDeviceService.findSendableDeviceIds(userProfileId)) {
        try {
          sendToDevice(reviewDate, userProfileId, pushDeviceId);
        } catch (PushNotificationException exception) {
          if (firstFailure == null) {
            firstFailure = exception;
          }
        }
      }
    }
    if (firstFailure != null) {
      throw firstFailure;
    }
  }

  /** 기준 날짜에 이미 Ticket을 접수한 이력의 Receipt 확인을 먼저 예약한다. */
  private void scheduleAcceptedDeliveryReceipts(LocalDate reviewDate) {
    pushDeliveryService
        .findAcceptedDeliveryIds("review-reminder:" + reviewDate + ":")
        .forEach(pushDeliveryId -> pushQueuePublisher.scheduleReceiptCheck(pushDeliveryId, 1));
  }

  /** 현재 발송 대상 설치의 새 발송 또는 안전한 재시도를 선점한다. */
  private void sendToDevice(LocalDate reviewDate, Long userProfileId, Long pushDeviceId) {
    PreparePushDeliveryCommand command = command(reviewDate, userProfileId, pushDeviceId);
    pushDeliveryService.prepare(command).ifPresent(this::sendPreparedDelivery);
  }

  /** 선점된 알림을 Expo에 보내고 Ticket 결과와 Receipt 예약을 기록한다. */
  private void sendPreparedDelivery(PreparedPushDelivery delivery) {
    PushTicketResult result;
    try {
      result = notificationSender.send(delivery.toPushMessage());
    } catch (RetryablePushNotificationException exception) {
      pushDeliveryService.markRetryable(delivery.pushDeliveryId());
      throw exception;
    }
    pushDeliveryService.recordTicketResult(delivery.pushDeliveryId(), result);
    if (result.accepted()) {
      pushQueuePublisher.scheduleReceiptCheck(delivery.pushDeliveryId(), 1);
    }
  }

  /** 날짜·사용자·설치 조합의 복습 리마인더 발송 명령을 생성한다. */
  private PreparePushDeliveryCommand command(
      LocalDate reviewDate, Long userProfileId, Long pushDeviceId) {
    return new PreparePushDeliveryCommand(
        userProfileId,
        pushDeviceId,
        NotificationType.REVIEW_REMINDER,
        "review-reminder:" + reviewDate + ":" + userProfileId + ":" + pushDeviceId,
        TITLE,
        BODY,
        DEEP_LINK);
  }
}
