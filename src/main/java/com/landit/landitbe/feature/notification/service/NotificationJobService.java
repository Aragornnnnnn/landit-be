// 체험 예약 의도와 관리자 이메일 테스트 및 발송 소유권을 관리한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.config.notification.EmailProperties;
import com.landit.landitbe.config.notification.TrialReminderProperties;
import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.notification.dto.NotificationJob;
import com.landit.landitbe.feature.notification.dto.NotificationJobView;
import com.landit.landitbe.feature.notification.dto.TrialReminderSettings;
import com.landit.landitbe.feature.notification.repository.NotificationJobRepository;
import com.landit.landitbe.feature.profile.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.dto.UserSubscriptionSnapshot;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 예약 의도를 구독 웹훅과 같은 트랜잭션에 기록한다. */
@Service
@RequiredArgsConstructor
public class NotificationJobService {
  private final NotificationJobRepository repository;
  private final UserProfileService profiles;
  private final TrialReminderProperties policy;
  private final EmailProperties email;
  private final AdminAuditService audit;
  private final Clock clock;

  @Value("${landit.notification.consumer-enabled:false}")
  private boolean consumerEnabled;

  /**
   * 반영된 구독 상태가 연간 무료 체험이면 두 채널의 발송 의도를 저장한다.
   *
   * @param userId 프로필 잠금을 획득한 대상 사용자 ID
   * @param environment RevenueCat 환경
   */
  @Transactional
  public void recordTrial(long userId, String environment) {
    if (!"PRODUCTION".equals(environment)
        && !(policy.sandboxEnabled() && "SANDBOX".equals(environment))) {
      return;
    }
    profiles
        .findSubscriptionNotificationTarget(userId)
        .ifPresent(
            target -> {
              UserSubscriptionSnapshot snapshot = target.subscription();
              if (!eligible(snapshot)) {
                return;
              }
              Instant expires = snapshot.expiresAt().atZone(ZoneId.of("Asia/Seoul")).toInstant();
              if (!expires.isAfter(clock.instant())) {
                return;
              }
              for (String kind : List.of("TRIAL_PUSH", "TRIAL_EMAIL")) {
                String key =
                    kind
                        + ":"
                        + userId
                        + ":"
                        + snapshot.productId()
                        + ":"
                        + snapshot.store()
                        + ":"
                        + environment
                        + ":"
                        + expires;
                UUID id = key(key);
                if (repository.find(id).isEmpty()) {
                  repository.insert(
                      new NotificationJob(
                          id,
                          kind,
                          userId,
                          snapshot.productId(),
                          snapshot.store().name(),
                          environment,
                          expires,
                          expires.minus(policy.leadTime()),
                          null,
                          "PENDING",
                          null,
                          null,
                          null,
                          null),
                      clock.instant());
                }
              }
            });
  }

  /**
   * 연간 상품의 활성 무료 체험인지 확인한다.
   *
   * @param snapshot 현재 구독
   * @return 발송 대상 여부
   */
  public boolean eligible(UserSubscriptionSnapshot snapshot) {
    return snapshot.subscriptionStatus() == SubscriptionStatus.ACTIVE
        && snapshot.periodType() == SubscriptionPeriodType.TRIAL
        && snapshot.productId() != null
        && policy.annualProductIds().contains(snapshot.productId())
        && snapshot.expiresAt() != null
        && snapshot.store() != null
        && List.of("APP_STORE", "PLAY_STORE").contains(snapshot.store().name());
  }

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
        throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
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
   * 발송 작업을 조회한다.
   *
   * @param id 작업 ID
   * @return 작업이 존재하면 현재 상태
   */
  public Optional<NotificationJob> find(UUID id) {
    return repository.find(id);
  }

  /**
   * 외부 예약이 필요한 미등록 작업을 반환한다.
   *
   * @return 최대 100개 작업
   */
  public List<NotificationJob> pendingReservations() {
    return repository.pendingReservations(clock.instant());
  }

  /**
   * 예약 등록을 짧게 선점한다.
   *
   * @param id 작업 ID
   * @return 예약 소유 여부
   */
  public boolean reserve(UUID id) {
    return repository.reserve(id, clock.instant());
  }

  /**
   * 외부 예약 성공을 기록한다.
   *
   * @param id 작업 ID
   */
  public void registered(UUID id) {
    repository.registered(id);
  }

  /**
   * 채널 작업을 선점하고 중단된 이메일의 불확실성을 기록한다.
   *
   * @param id 작업 ID
   * @return 처리할 작업. 다른 실행이 처리 중이거나 완료했으면 빈 값
   */
  @Transactional
  public Optional<NotificationJob> claim(UUID id) {
    repository.recoverUnknownEmail(id, clock.instant());
    if (!repository.claim(id, UUID.randomUUID(), clock.instant())) {
      return Optional.empty();
    }
    return repository.find(id);
  }

  /**
   * 처리 소유권에 맞는 결과를 저장한다.
   *
   * @param job 선점한 작업
   * @param status 결과 상태
   * @param code 결과 코드
   * @param messageId 제공자 접수 ID
   */
  public void finish(NotificationJob job, String status, String code, String messageId) {
    repository.finish(job, status, code, messageId);
  }

  /**
   * 현재 채널 설정을 조회한다.
   *
   * @return 설정
   */
  public TrialReminderSettings settings() {
    return repository.settings();
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
