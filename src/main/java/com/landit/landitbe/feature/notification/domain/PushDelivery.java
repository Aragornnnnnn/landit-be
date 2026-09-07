// 푸시 알림 요청과 Expo Ticket·Receipt 결과를 저장한다.

package com.landit.landitbe.feature.notification.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

/** 푸시 알림 요청과 Expo Ticket·Receipt 결과를 저장한다. */
@Getter
@Entity
@Table(name = "push_delivery")
public class PushDelivery extends BaseTimeEntity {

  private static final String RETRYABLE_ERROR_CODE = "TEMPORARY_PROVIDER_FAILURE";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "user_push_token_id", nullable = false)
  private Long userPushTokenId;

  @Column(name = "sent_expo_push_token", nullable = false, length = 500)
  private String sentExpoPushToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 40)
  private NotificationType notificationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_variant", length = 40)
  private NotificationContentVariant contentVariant;

  @Column(name = "deduplication_key", nullable = false, length = 255, unique = true)
  private String deduplicationKey;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 500)
  private String body;

  @Column(name = "deep_link", nullable = false, length = 1000)
  private String deepLink;

  @Column(name = "expo_ticket_id", length = 255)
  private String expoTicketId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PushDeliveryStatus status;

  @Column(name = "error_code", length = 100)
  private String errorCode;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "receipt_checked_at")
  private LocalDateTime receiptCheckedAt;

  @Column(name = "admin_push_target_id")
  private Long adminPushTargetId;

  @Column(name = "admin_retry_count", nullable = false)
  private int adminRetryCount;

  @Column(name = "admin_next_attempt_at")
  private LocalDateTime adminNextAttemptAt;

  @Column(name = "admin_receipt_attempt", nullable = false)
  private int adminReceiptAttempt;

  @Column(name = "admin_receipt_next_at")
  private LocalDateTime adminReceiptNextAt;

  @Column(name = "admin_receipt_lease_until")
  private LocalDateTime adminReceiptLeaseUntil;

  /**
   * 새 이력을 확정된 관리자 발송 대상에 연결한다.
   *
   * @param targetId 대상 ID
   */
  public void attachAdminTarget(long targetId) {
    adminPushTargetId = targetId;
  }

  /**
   * 관리자 발송의 결과 불명을 기록하고 자동 재제출을 차단한다.
   *
   * @param code 토큰 원문 없는 사유
   */
  public void adminUnknown(String code) {
    if (adminPushTargetId != null
        && (status == PushDeliveryStatus.REQUESTED
            || status == PushDeliveryStatus.TICKET_ACCEPTED)) {
      status = PushDeliveryStatus.UNKNOWN;
      errorCode = code;
      adminNextAttemptAt = null;
      adminReceiptNextAt = null;
    }
  }

  /**
   * 명시적으로 거부된 429 요청의 재시도를 제한한다.
   *
   * @param now 현재 시각
   */
  public void adminRateLimited(LocalDateTime now) {
    if (status != PushDeliveryStatus.REQUESTED || adminPushTargetId == null) {
      return;
    }
    if (adminRetryCount >= 3) {
      fail("RATE_LIMIT_RETRIES_EXHAUSTED", now);
      return;
    }
    int[] delays = {5, 30, 120};
    adminNextAttemptAt = now.plusSeconds(delays[adminRetryCount++]);
    errorCode = "ADMIN_RATE_LIMITED";
  }

  /**
   * DB에 저장한 관리자 Receipt 확인 회차를 선점한다.
   *
   * @param now 현재 시각
   * @return 선점한 경우 true
   */
  public boolean claimAdminReceipt(LocalDateTime now) {
    if (adminPushTargetId == null
        || status != PushDeliveryStatus.TICKET_ACCEPTED
        || adminReceiptNextAt == null
        || adminReceiptNextAt.isAfter(now)
        || (adminReceiptLeaseUntil != null && adminReceiptLeaseUntil.isAfter(now))) {
      return false;
    }
    if (adminReceiptAttempt >= 3) {
      adminUnknown("ReceiptNotAvailable");
      return false;
    }
    adminReceiptAttempt++;
    adminReceiptLeaseUntil = now.plusMinutes(5);
    return true;
  }

  /**
   * 미준비 또는 통신 실패 Receipt의 다음 회차를 예약한다.
   *
   * @param attempt 선점한 확인 회차
   * @param now 현재 시각
   */
  public void deferAdminReceipt(int attempt, LocalDateTime now) {
    if (adminReceiptAttempt != attempt || status != PushDeliveryStatus.TICKET_ACCEPTED) {
      return;
    }
    adminReceiptLeaseUntil = null;
    if (adminReceiptAttempt >= 3) {
      adminUnknown("ReceiptNotAvailable");
    } else {
      adminReceiptNextAt = now.plusMinutes(15);
    }
  }

  /** JPA에서 사용하는 기본 생성자다. */
  protected PushDelivery() {}

  /** Expo 요청 전 선점할 발송 정보와 요청 시각을 받아 발송 이력을 생성한다. */
  private PushDelivery(
      Long userProfileId,
      Long userPushTokenId,
      String sentExpoPushToken,
      NotificationType notificationType,
      NotificationContentVariant contentVariant,
      String deduplicationKey,
      String title,
      String body,
      String deepLink,
      LocalDateTime requestedAt) {
    this.userProfileId = userProfileId;
    this.userPushTokenId = userPushTokenId;
    this.sentExpoPushToken = sentExpoPushToken;
    this.notificationType = notificationType;
    this.contentVariant = contentVariant;
    this.deduplicationKey = deduplicationKey;
    this.title = title;
    this.body = body;
    this.deepLink = deepLink;
    this.requestedAt = requestedAt;
    this.status = PushDeliveryStatus.REQUESTED;
  }

  /**
   * Expo에 발송을 요청하기 전 멱등 발송 이력을 생성한다.
   *
   * @param userProfileId 발송 대상 사용자 식별자
   * @param userPushTokenId 발송 대상 사용자 Push Token 식별자
   * @param sentExpoPushToken 이번 발송에 사용한 Expo Push Token
   * @param notificationType 발송 알림 유형
   * @param contentVariant 알림 문구 변형. 정책이 없는 수동 발송은 {@code null}
   * @param deduplicationKey 중복 발송 방지 키
   * @param title 알림 제목
   * @param body 알림 본문
   * @param deepLink 알림 진입 경로
   * @param requestedAt Expo 요청 시각
   * @return 요청 상태의 발송 이력
   */
  public static PushDelivery requested(
      Long userProfileId,
      Long userPushTokenId,
      String sentExpoPushToken,
      NotificationType notificationType,
      NotificationContentVariant contentVariant,
      String deduplicationKey,
      String title,
      String body,
      String deepLink,
      LocalDateTime requestedAt) {
    return new PushDelivery(
        userProfileId,
        userPushTokenId,
        sentExpoPushToken,
        notificationType,
        contentVariant,
        deduplicationKey,
        title,
        body,
        deepLink,
        requestedAt);
  }

  /** 기존 호출자의 문구 변형 없는 발송 이력을 생성한다. */
  public static PushDelivery requested(
      Long userProfileId,
      Long userPushTokenId,
      String sentExpoPushToken,
      NotificationType notificationType,
      String deduplicationKey,
      String title,
      String body,
      String deepLink,
      LocalDateTime requestedAt) {
    return requested(
        userProfileId,
        userPushTokenId,
        sentExpoPushToken,
        notificationType,
        null,
        deduplicationKey,
        title,
        body,
        deepLink,
        requestedAt);
  }

  /**
   * 요청 상태라면 Expo가 접수한 Ticket ID를 기록한다.
   *
   * @param ticketId Expo Ticket ID
   * @return Ticket 접수 상태로 전환했으면 {@code true}
   */
  public boolean acceptTicket(String ticketId) {
    if (ticketId == null || ticketId.isBlank()) {
      throw new IllegalArgumentException("Expo Ticket ID는 비어 있을 수 없습니다.");
    }
    if (status != PushDeliveryStatus.REQUESTED
        && !(adminPushTargetId != null && status == PushDeliveryStatus.UNKNOWN)) {
      return false;
    }
    expoTicketId = ticketId;
    if (adminPushTargetId != null) {
      adminReceiptNextAt = LocalDateTime.now().plusMinutes(15);
    }
    status = PushDeliveryStatus.TICKET_ACCEPTED;
    errorCode = null;
    return true;
  }

  /** 외부 Push 제공자의 일시 오류를 같은 발송 이력으로 재시도할 수 있게 표시한다. */
  public void markRetryable() {
    if (status == PushDeliveryStatus.REQUESTED) {
      errorCode = RETRYABLE_ERROR_CODE;
    }
  }

  /**
   * 현재 발송 이력이 명시적인 일시 오류 이후 재시도를 기다리는지 반환한다.
   *
   * @return 재시도 표식이 남아 있으면 {@code true}
   */
  public boolean isRetryable() {
    return status == PushDeliveryStatus.REQUESTED
        && (RETRYABLE_ERROR_CODE.equals(errorCode)
            || (adminPushTargetId != null
                && "ADMIN_RATE_LIMITED".equals(errorCode)
                && adminNextAttemptAt != null
                && !adminNextAttemptAt.isAfter(LocalDateTime.now())));
  }

  /**
   * 재시도 표식을 원자적으로 소비한다.
   *
   * @return 현재 재시도를 선점했으면 {@code true}
   */
  public boolean claimRetry() {
    if (!isRetryable()) {
      return false;
    }
    errorCode = null;
    if (adminPushTargetId != null) {
      adminNextAttemptAt = null;
      requestedAt = LocalDateTime.now();
    }
    return true;
  }

  /**
   * 요청 상태라면 Ticket 오류를 기록한다.
   *
   * @param failureCode Expo Ticket 오류 코드
   * @param checkedAt 결과 기록 시각
   * @return Ticket 실패 상태로 전환했으면 {@code true}
   */
  public boolean failTicket(String failureCode, LocalDateTime checkedAt) {
    if (status != PushDeliveryStatus.REQUESTED) {
      return false;
    }
    fail(failureCode, checkedAt);
    return true;
  }

  /**
   * Ticket 접수 상태라면 Expo Receipt 배달 성공 상태로 전환한다.
   *
   * @param checkedAt Receipt 확인 시각
   * @return 배달 완료 상태로 전환했으면 {@code true}
   */
  public boolean delivered(LocalDateTime checkedAt) {
    if (status != PushDeliveryStatus.TICKET_ACCEPTED
        && !(adminPushTargetId != null
            && status == PushDeliveryStatus.UNKNOWN
            && expoTicketId != null)) {
      return false;
    }
    status = PushDeliveryStatus.DELIVERED;
    errorCode = null;
    receiptCheckedAt = checkedAt;
    return true;
  }

  /**
   * Ticket 접수 상태라면 Receipt 오류를 기록한다.
   *
   * @param failureCode Expo Receipt 오류 코드
   * @param checkedAt Receipt 확인 시각
   * @return Receipt 실패 상태로 전환했으면 {@code true}
   */
  public boolean failReceipt(String failureCode, LocalDateTime checkedAt) {
    if (status != PushDeliveryStatus.TICKET_ACCEPTED
        && !(adminPushTargetId != null
            && status == PushDeliveryStatus.UNKNOWN
            && expoTicketId != null)) {
      return false;
    }
    fail(failureCode, checkedAt);
    return true;
  }

  /** 최종 실패 상태와 오류 코드, 결과 기록 시각을 저장한다. */
  private void fail(String failureCode, LocalDateTime checkedAt) {
    status = PushDeliveryStatus.FAILED;
    errorCode = failureCode;
    receiptCheckedAt = checkedAt;
  }
}
