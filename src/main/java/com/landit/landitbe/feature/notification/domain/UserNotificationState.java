// 사용자별 마지막 알림 대상 계산 결과를 저장한다.

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

/** 사용자별 마지막 알림 대상 계산 결과를 저장한다. */
@Getter
@Entity
@Table(name = "user_notification_state")
public class UserNotificationState extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false, unique = true)
  private Long userProfileId;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 40)
  private NotificationType notificationType;

  @Column(name = "target_id")
  private Long targetId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationStateStatus status;

  @Column(name = "last_activity_at")
  private LocalDateTime lastActivityAt;

  @Column(name = "last_sent_at")
  private LocalDateTime lastSentAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserNotificationState() {}

  private UserNotificationState(
      Long userProfileId,
      NotificationType notificationType,
      Long targetId,
      LocalDateTime lastActivityAt) {
    refresh(notificationType, targetId, lastActivityAt);
    this.userProfileId = userProfileId;
  }

  /**
   * 계산한 알림 대상 상태를 생성한다.
   *
   * @param userProfileId 알림 대상 사용자 ID
   * @param notificationType 계산된 알림 유형
   * @param targetId 알림 클릭 시 이동할 콘텐츠 ID
   * @param lastActivityAt 대상 선정에 사용한 마지막 실제 완료 시각
   * @return 발행 전 상태의 알림 계산 결과
   */
  public static UserNotificationState ready(
      Long userProfileId,
      NotificationType notificationType,
      Long targetId,
      LocalDateTime lastActivityAt) {
    return new UserNotificationState(userProfileId, notificationType, targetId, lastActivityAt);
  }

  /**
   * 최신 계산 결과로 대상과 상태를 갱신한다.
   *
   * @param notificationType 계산된 알림 유형
   * @param targetId 알림 클릭 시 이동할 콘텐츠 ID
   * @param lastActivityAt 대상 선정에 사용한 마지막 실제 완료 시각
   */
  public void refresh(
      NotificationType notificationType, Long targetId, LocalDateTime lastActivityAt) {
    this.notificationType = notificationType;
    this.targetId = targetId;
    this.lastActivityAt = lastActivityAt;
    this.status = NotificationStateStatus.READY;
  }

  /**
   * 계산 결과에 해당하는 알림 발행 시각을 기록한다.
   *
   * @param sentAt Push 발송 메시지를 Queue에 발행한 시각
   */
  public void markSent(LocalDateTime sentAt) {
    this.status = NotificationStateStatus.SENT;
    this.lastSentAt = sentAt;
  }
}
