// 별도 Push SQS 배치에서 복습을 생성하고 기존 발송 경로로 전달한다.

package com.landit.landitbe.feature.notification.scheduled.service;

import com.landit.landitbe.feature.learning.review.dto.ReviewOffer;
import com.landit.landitbe.feature.learning.review.service.ExpressionReviewService;
import com.landit.landitbe.feature.notification.delivery.dto.SendPushNotificationCommand;
import com.landit.landitbe.feature.notification.delivery.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.token.service.UserPushTokenDeliveryService;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 복습 알림을 반복 처리해도 날짜·사용자 이벤트 키와 고정 복습을 재사용한다. */
@Slf4j
@Service
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class ReviewNotificationService {
  private final ExpressionReviewService reviews;
  private final LearningNotificationFrequencyService frequency;
  private final UserPushTokenDeliveryService tokens;
  private final NotificationDispatchService dispatch;
  private final Clock clock;
  private final TransactionTemplate transactions;

  /**
   * 기존 Push 발송 구성에 복습 배치를 연결한다.
   *
   * @param reviews 복습 생성 업무
   * @param frequency 학습 알림 빈도 정책
   * @param tokens 활성 기기 조회
   * @param dispatch 멱등 Push 발송
   * @param clock 기준 시계
   * @param transactionManager 복습 생성과 슬롯 예약의 트랜잭션 관리자
   */
  public ReviewNotificationService(
      ExpressionReviewService reviews,
      LearningNotificationFrequencyService frequency,
      UserPushTokenDeliveryService tokens,
      NotificationDispatchService dispatch,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this.reviews = reviews;
    this.frequency = frequency;
    this.tokens = tokens;
    this.dispatch = dispatch;
    this.clock = clock;
    this.transactions = new TransactionTemplate(transactionManager);
    this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * 같은 날짜의 배치만 처리하고 100명 단위로 SQS visibility를 연장한다.
   *
   * @param messageId 로그 추적용 메시지 ID
   * @param occurredAt Scheduler 예정 시각
   * @param visibilityExtender SQS visibility 연장
   */
  public void process(String messageId, Instant occurredAt, Runnable visibilityExtender) {
    LocalDate date = occurredAt.atZone(clock.getZone()).toLocalDate();
    if (!date.equals(LocalDate.now(clock))) {
      log.info("review_notification_skipped reason=stale_or_future messageId={}", messageId);
      return;
    }
    long cursor = 0;
    int selected = 0;
    while (true) {
      visibilityExtender.run();
      List<Long> users = reviews.candidateUsers(cursor, 100);
      if (users.isEmpty()) {
        log.info(
            "review_notification_completed messageId={} selectedUsers={}", messageId, selected);
        return;
      }
      var sendable = tokens.findSendableTokenIdsByUserProfileIds(users);
      List<SendPushNotificationCommand> commands = new ArrayList<>();
      for (long userId : users) {
        if (!sendable.getOrDefault(userId, List.of()).isEmpty()) {
          commands.addAll(prepare(userId, date));
        }
      }
      visibilityExtender.run();
      dispatch.sendAll(commands);
      selected += commands.size();
      cursor = users.getLast();
    }
  }

  private List<SendPushNotificationCommand> prepare(long userId, LocalDate date) {
    try {
      return transactions.execute(
          status -> {
            var offer = reviews.offer(userId, date);
            if (offer.isEmpty()) {
              return List.of();
            }
            var reserved = frequency.reserveAll(List.of(command(offer.get(), date)));
            if (reserved.isEmpty()) {
              // 실제로 발송하지 못한 복습은 생성·출제 간격을 소비하지 않는다.
              status.setRollbackOnly();
            }
            return reserved;
          });
    } catch (UserProfileException exception) {
      if (exception.getErrorCode() == UserProfileErrorCode.INVALID_TOKEN) {
        return List.of();
      }
      throw exception;
    }
  }

  private SendPushNotificationCommand command(ReviewOffer offer, LocalDate date) {
    return new SendPushNotificationCommand(
        "review:" + date + ":" + offer.userId(),
        offer.userId(),
        NotificationType.EXPRESSION_REVIEW,
        "배웠던 표현, 다시 꺼내 볼까요?",
        "표현 " + offer.questionCount() + "개를 짧은 퀴즈로 복습해 보세요.",
        "/reviews/"
            + offer.reviewId()
            + "?utm_source=push&utm_medium=notification&utm_campaign=expression_review");
  }
}
