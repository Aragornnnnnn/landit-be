// 예약된 체험 푸시와 이메일을 현재 상태 확인 후 채널별로 발송한다.

package com.landit.landitbe.feature.notification.job.service;

import com.landit.landitbe.config.notification.TrialReminderProperties;
import com.landit.landitbe.feature.notification.delivery.client.RetryablePushNotificationException;
import com.landit.landitbe.feature.notification.delivery.dto.SendPushNotificationCommand;
import com.landit.landitbe.feature.notification.delivery.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.email.client.EmailSendResult.Status;
import com.landit.landitbe.feature.notification.email.client.EmailSender;
import com.landit.landitbe.feature.notification.email.dto.EmailRecipient;
import com.landit.landitbe.feature.notification.email.service.NotificationEmailTemplateService;
import com.landit.landitbe.feature.notification.job.dto.NotificationJob;
import com.landit.landitbe.feature.profile.subscription.dto.SubscriptionNotificationTarget;
import com.landit.landitbe.feature.profile.subscription.service.ProfileSubscriptionService;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 이메일 실패가 이미 보낸 푸시를 재발송하지 않도록 채널별 작업을 처리한다. */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "landit.notification",
    name = "consumer-enabled",
    havingValue = "true")
public class NotificationJobProcessingService {
  private final NotificationJobService jobs;
  private final ProfileSubscriptionService profiles;
  private final NotificationDispatchService push;
  private final EmailSender sender;
  private final NotificationEmailTemplateService emailTemplate;
  private final TrialReminderProperties policy;
  private final Clock clock;
  private final Validator validator;

  /**
   * 작업을 선점하고 발송하거나 제외 사유를 기록한다.
   *
   * @param id 예약 작업 ID
   */
  public void process(UUID id) {
    Optional<NotificationJob> existing = jobs.find(id);
    if (existing.isEmpty()) {
      return;
    }
    if (existing.get().scheduledAt().isAfter(clock.instant())) {
      throw new RetryablePushNotificationException("알림 예정 시각 전입니다.");
    }
    Optional<NotificationJob> claimed = jobs.claim(id);
    if (claimed.isEmpty()) {
      if (jobs.find(id).map(job -> job.status().equals("PROCESSING")).orElse(false)) {
        throw new RetryablePushNotificationException("다른 실행에서 발송 중입니다.");
      }
      return;
    }
    NotificationJob job = claimed.get();
    try {
      deliver(job);
    } catch (RetryablePushNotificationException exception) {
      jobs.finish(job, "PENDING", "RETRYABLE", null);
      throw exception;
    }
  }

  private void deliver(NotificationJob job) {
    if (!job.scheduledAt()
        .plus(job.trial() ? policy.maxLateness() : Duration.ofHours(1))
        .isAfter(clock.instant())) {
      skip(job, "TOO_LATE");
      return;
    }
    Optional<SubscriptionNotificationTarget> target =
        profiles.findSubscriptionNotificationTarget(job.userProfileId());
    if (target.isEmpty()) {
      skip(job, "INACTIVE_USER");
      return;
    }
    if (job.trial() && !currentTrial(job, target.get())) {
      skip(job, "TRIAL_CHANGED");
      return;
    }
    if (job.kind().equals("TRIAL_PUSH")) {
      sendPush(job, target.get());
    } else {
      sendEmail(job, job.trial() ? target.get().email() : job.recipient());
    }
  }

  private boolean currentTrial(NotificationJob job, SubscriptionNotificationTarget target) {
    var snapshot = target.subscription();
    return jobs.eligible(snapshot)
        && Objects.equals(job.productId(), snapshot.productId())
        && Objects.equals(job.store(), snapshot.store().name())
        && job.expiresAt().isAfter(clock.instant())
        && job.expiresAt().equals(snapshot.expiresAt().atZone(ZoneId.of("Asia/Seoul")).toInstant());
  }

  private void sendPush(NotificationJob job, SubscriptionNotificationTarget target) {
    if (!jobs.settings().pushEnabled()) {
      skip(job, "CHANNEL_DISABLED");
      return;
    }
    if (!target.pushGranted()) {
      skip(job, "PUSH_PERMISSION_DENIED");
      return;
    }
    push.sendAll(
        List.of(
            new SendPushNotificationCommand(
                job.id().toString(),
                job.userProfileId(),
                NotificationType.TRIAL_ENDING,
                "무료 체험 종료 예정 안내",
                "무료 체험이 곧 종료돼요. 결제 전 구독 정보를 확인해 주세요.",
                "/me/subscription")));
    // 중복 재시도에서 신규 Ticket이 0개여도 기존 Push Delivery가 최종 전달 상태를 소유한다.
    jobs.finish(job, "PROCESSED", "PUSH_DELIVERY_TRACKED", null);
  }

  private void sendEmail(NotificationJob job, String recipient) {
    if (job.trial() && !jobs.settings().emailEnabled()) {
      skip(job, "CHANNEL_DISABLED");
      return;
    }
    if (!validator.validate(new EmailRecipient(recipient)).isEmpty()) {
      skip(job, "NO_VALID_EMAIL");
      return;
    }
    String subject = job.trial() ? "[Landit] 무료 체험 종료 예정 안내" : "[Landit] 이메일 발송 테스트";
    String message = job.trial() ? body(job) : "Landit 관리자 화면에서 요청한 이메일 발송 테스트입니다.";
    String url = job.trial() ? managementUrl(job) : null;
    String text = message + (url == null ? "" : "\n\n구독 관리: " + url);
    text += "\n\n" + NotificationEmailTemplateService.FOOTER;
    var result = sender.send(recipient, subject, text, emailTemplate.render(subject, message, url));
    if (result.status() == Status.RETRYABLE) {
      throw new RetryablePushNotificationException("이메일 제공자가 일시적으로 접수를 거절했습니다.");
    }
    jobs.finish(job, result.status().name(), "SES_" + result.status(), result.providerMessageId());
    if (result.status() != Status.ACCEPTED) {
      log.warn("이메일 접수 확인 필요: jobId={}, status={}", job.id(), result.status());
    }
  }

  private String body(NotificationJob job) {
    String expiry =
        DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")
            .withZone(ZoneId.of("Asia/Seoul"))
            .format(job.expiresAt());
    return "무료 체험이 "
        + expiry
        + "(한국 시간)에 종료될 예정이에요. "
        + "이후 연간 구독의 유료 전환 여부와 결제 금액은 스토어 구독 화면에서 확인해 주세요. "
        + "유료 이용을 원하지 않으면 스토어에서 미리 구독을 취소해 주세요.";
  }

  private String managementUrl(NotificationJob job) {
    return job.store().equals("APP_STORE")
        ? "https://apps.apple.com/account/subscriptions"
        : "https://play.google.com/store/account/subscriptions";
  }

  private void skip(NotificationJob job, String reason) {
    jobs.finish(job, "SKIPPED", reason, null);
  }
}
