// 관리자 이메일 테스트 접수와 체험 알림 채널 설정 변경을 감사 기록과 함께 처리한다.

package com.landit.landitbe.feature.notification.job.admin.service;

import com.landit.landitbe.config.notification.EmailProperties;
import com.landit.landitbe.config.notification.TrialReminderProperties;
import com.landit.landitbe.feature.audit.domain.AdminAction;
import com.landit.landitbe.feature.audit.service.AdminAuditService;
import com.landit.landitbe.feature.notification.exception.NotificationErrorCode;
import com.landit.landitbe.feature.notification.job.dto.NotificationJob;
import com.landit.landitbe.feature.notification.job.dto.NotificationJobView;
import com.landit.landitbe.feature.notification.job.dto.TrialReminderSettings;
import com.landit.landitbe.feature.notification.job.repository.NotificationJobRepository;
import com.landit.landitbe.feature.profile.authentication.service.ProfileAuthenticationService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 전용 알림 작업과 설정 변경을 소유한다. */
@Service
@RequiredArgsConstructor
public class AdminNotificationJobService {
  private final NotificationJobRepository repository;
  private final ProfileAuthenticationService profiles;
  private final TrialReminderProperties policy;
  private final EmailProperties email;
  private final AdminAuditService audit;
  private final Clock clock;

  @Value("${landit.notification.consumer-enabled:false}")
  private boolean consumerEnabled;

  /**
   * 관리자별 요청 키로 임의 이메일 테스트를 한 번만 접수한다.
   *
   * @param adminId 인증 관리자 ID
   * @param requestKey UUID 형식의 요청 멱등 키
   * @param recipient 검증된 형식의 수신 주소
   * @return 접수 작업
   */
  @Transactional
  public NotificationJobView requestTest(long adminId, UUID requestKey, String recipient) {
    requireEmail();
    profiles
        .findAuthenticationProfileForUpdate(adminId)
        .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
    UUID id = key("TEST_EMAIL:" + adminId + ":" + requestKey);
    Optional<NotificationJob> previous = repository.find(id);
    if (previous.isPresent()) {
      if (!recipient.equals(previous.get().recipient())) {
        throw new ApiException(NotificationErrorCode.IDEMPOTENCY_KEY_CONFLICT);
      }
      return previous.get().view();
    }
    Instant now = clock.instant();
    NotificationJob job =
        new NotificationJob(
            id,
            "TEST_EMAIL",
            adminId,
            null,
            null,
            null,
            null,
            now,
            recipient,
            "PENDING",
            null,
            null,
            null,
            null);
    repository.insert(job, now);
    audit.record(
        adminId,
        AdminAction.EMAIL_TEST_REQUESTED,
        "NOTIFICATION_JOB",
        id.toString(),
        null,
        "PENDING");
    return job.view();
  }

  /**
   * 관리자 변경을 감사 기록과 함께 저장한다.
   *
   * @param adminId 관리자 ID
   * @param settings 변경할 설정
   * @return 저장된 설정
   */
  @Transactional
  public TrialReminderSettings updateSettings(long adminId, TrialReminderSettings settings) {
    if ((settings.pushEnabled() || settings.emailEnabled())
        && (!consumerEnabled || policy.annualProductIds().isEmpty())) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
    }
    if (settings.emailEnabled()) {
      requireEmail();
    }
    TrialReminderSettings before = repository.settings();
    repository.updateSettings(settings);
    audit.record(
        adminId,
        AdminAction.TRIAL_REMINDER_SETTINGS_UPDATED,
        "TRIAL_REMINDER",
        "1",
        before.toString(),
        settings.toString());
    return settings;
  }

  private void requireEmail() {
    if (!consumerEnabled || email.from().isBlank()) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
    }
  }

  private UUID key(String key) {
    return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
  }
}
