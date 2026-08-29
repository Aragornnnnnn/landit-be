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

  @Column(name = "push_device_id", nullable = false)
  private Long pushDeviceId;

  @Column(name = "sent_expo_push_token", nullable = false, length = 500)
  private String sentExpoPushToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 40)
  private NotificationType notificationType;

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

  /** JPA에서 사용하는 기본 생성자다. */
  protected PushDelivery() {}

  /** Expo 요청 전 선점할 발송 정보와 요청 시각을 받아 발송 이력을 생성한다. */
  private PushDelivery(
      Long userProfileId,
      Long pushDeviceId,
      String sentExpoPushToken,
      NotificationType notificationType,
      String deduplicationKey,
      String title,
      String body,
      String deepLink,
      LocalDateTime requestedAt) {
    this.userProfileId = userProfileId;
    this.pushDeviceId = pushDeviceId;
    this.sentExpoPushToken = sentExpoPushToken;
    this.notificationType = notificationType;
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
   * @param pushDeviceId 발송 대상 기기 식별자
   * @param sentExpoPushToken 이번 발송에 사용한 Expo Push Token
   * @param notificationType 발송 알림 유형
   * @param deduplicationKey 중복 발송 방지 키
   * @param title 알림 제목
   * @param body 알림 본문
   * @param deepLink 알림 진입 경로
   * @param requestedAt Expo 요청 시각
   * @return 요청 상태의 발송 이력
   */
  public static PushDelivery requested(
      Long userProfileId,
      Long pushDeviceId,
      String sentExpoPushToken,
      NotificationType notificationType,
      String deduplicationKey,
      String title,
      String body,
      String deepLink,
      LocalDateTime requestedAt) {
    return new PushDelivery(
        userProfileId,
        pushDeviceId,
        sentExpoPushToken,
        notificationType,
        deduplicationKey,
        title,
        body,
        deepLink,
        requestedAt);
  }

  /** Expo가 접수한 Ticket ID를 기록한다. */
  public void acceptTicket(String ticketId) {
    expoTicketId = ticketId;
    status = PushDeliveryStatus.TICKET_ACCEPTED;
    errorCode = null;
  }

  /** 외부 Push 제공자의 일시 오류를 같은 발송 이력으로 재시도할 수 있게 표시한다. */
  public void markRetryable() {
    if (status == PushDeliveryStatus.REQUESTED) {
      errorCode = RETRYABLE_ERROR_CODE;
    }
  }

  /** 현재 발송 이력이 명시적인 일시 오류 이후 재시도를 기다리는지 반환한다. */
  public boolean isRetryable() {
    return status == PushDeliveryStatus.REQUESTED && RETRYABLE_ERROR_CODE.equals(errorCode);
  }

  /** Expo Receipt가 배달 성공을 확인한 상태로 전환한다. */
  public void delivered(LocalDateTime checkedAt) {
    status = PushDeliveryStatus.DELIVERED;
    errorCode = null;
    receiptCheckedAt = checkedAt;
  }

  /** Ticket 또는 Receipt 오류를 기록한다. */
  public void fail(String failureCode, LocalDateTime checkedAt) {
    status = PushDeliveryStatus.FAILED;
    errorCode = failureCode;
    receiptCheckedAt = checkedAt;
  }
}
