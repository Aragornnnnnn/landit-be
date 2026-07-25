// 기준 날짜의 복습 대상 설치에 리마인더를 멱등 발송한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.learning.service.ReviewItemService;
import com.landit.landitbe.feature.learning.service.ReviewReminderTargetPage;
import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
  private static final int TARGET_USER_PAGE_SIZE = 100;
  private static final int EXPO_BATCH_SIZE = 100;

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
    Long afterUserProfileId = null;
    do {
      ReviewReminderTargetPage targetPage =
          reviewItemService.findReminderTargetPage(
              reviewDate, afterUserProfileId, TARGET_USER_PAGE_SIZE);
      firstFailure =
          retainFirstFailure(firstFailure, sendTargetPage(reviewDate, targetPage.userProfileIds()));
      afterUserProfileId = targetPage.nextUserProfileId();
    } while (afterUserProfileId != null);
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

  /** 한 페이지의 발송 대상 설치를 선점한 뒤 Expo 요청 크기만큼 나누어 전송한다. */
  private PushNotificationException sendTargetPage(
      LocalDate reviewDate, List<Long> userProfileIds) {
    List<PreparedPushDelivery> deliveries = new ArrayList<>(EXPO_BATCH_SIZE);
    PushNotificationException firstFailure = null;
    for (PushDeviceSendTarget target :
        pushDeviceService.findSendableDeliveryTargets(userProfileIds)) {
      pushDeliveryService
          .prepare(command(reviewDate, target.userProfileId(), target.pushDeviceId()))
          .ifPresent(deliveries::add);
      if (deliveries.size() == EXPO_BATCH_SIZE) {
        firstFailure = retainFirstFailure(firstFailure, sendPreparedDeliveries(deliveries));
        deliveries.clear();
      }
    }
    if (!deliveries.isEmpty()) {
      firstFailure = retainFirstFailure(firstFailure, sendPreparedDeliveries(deliveries));
    }
    return firstFailure;
  }

  /** 선점된 알림 묶음을 Expo에 보내고 요청 순서대로 Ticket 결과와 Receipt 예약을 기록한다. */
  private PushNotificationException sendPreparedDeliveries(List<PreparedPushDelivery> deliveries) {
    try {
      List<PushTicketResult> results =
          notificationSender.send(
              deliveries.stream().map(PreparedPushDelivery::toPushMessage).toList());
      if (results.size() != deliveries.size()) {
        return new PushNotificationException("Expo Push Ticket 결과 수가 요청 수와 일치하지 않습니다.");
      }
      for (int index = 0; index < deliveries.size(); index++) {
        recordTicketResult(deliveries.get(index), results.get(index));
      }
      return null;
    } catch (RetryablePushNotificationException exception) {
      deliveries.forEach(delivery -> pushDeliveryService.markRetryable(delivery.pushDeliveryId()));
      return exception;
    } catch (PushNotificationException exception) {
      return exception;
    }
  }

  /** Ticket 결과를 발송 이력에 기록하고 접수된 알림의 Receipt 확인을 예약한다. */
  private void recordTicketResult(PreparedPushDelivery delivery, PushTicketResult result) {
    pushDeliveryService.recordTicketResult(delivery.pushDeliveryId(), result);
    if (result.accepted()) {
      pushQueuePublisher.scheduleReceiptCheck(delivery.pushDeliveryId(), 1);
    }
  }

  /** 먼저 발생한 실패를 유지해 모든 발송 대상 처리 뒤 SQS 재시도를 유도한다. */
  private PushNotificationException retainFirstFailure(
      PushNotificationException firstFailure, PushNotificationException failure) {
    return firstFailure == null ? failure : firstFailure;
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
