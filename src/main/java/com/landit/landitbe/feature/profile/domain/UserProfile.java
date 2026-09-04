// 서비스 사용자 프로필과 학습 기본 설정을 저장한다.

package com.landit.landitbe.feature.profile.domain;

import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import com.landit.landitbe.shared.domain.Locale;
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

/** 서비스 사용자 프로필과 학습 기본 설정을 저장한다. */
@Getter
@Entity
@Table(name = "user_profile")
public class UserProfile extends BaseTimeEntity {

  private static final Locale DEFAULT_TARGET_LOCALE = Locale.EN;
  private static final Locale DEFAULT_BASE_LOCALE = Locale.KR;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 255)
  private String email;

  @Column(nullable = false, length = 100)
  private String nickname;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_locale", nullable = false, length = 35)
  private Locale targetLocale;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Column(name = "learning_level")
  private Integer learningLevel;

  @Column(name = "current_level", nullable = false)
  private int currentLevel;

  @Column(name = "ai_tutor_id")
  private Long aiTutorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "accent_locale", nullable = false, length = 35)
  private AccentLocale accentLocale;

  @Enumerated(EnumType.STRING)
  @Column(name = "push_permission_status", nullable = false, length = 30)
  private PushPermissionStatus pushPermissionStatus;

  @Column(name = "push_permission_updated_at")
  private LocalDateTime pushPermissionUpdatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "subscription_status", nullable = false, length = 30)
  private SubscriptionStatus subscriptionStatus;

  @Column(name = "subscription_expires_at")
  private LocalDateTime subscriptionExpiresAt;

  @Column(name = "subscription_event_at")
  private LocalDateTime subscriptionEventAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserProfileStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserProfile() {}

  /**
   * 기본 학습 설정을 가진 활성 사용자 프로필을 생성한다.
   *
   * @param email 사용자 이메일
   * @param nickname 사용자 닉네임
   * @param aiTutorId 기본 AI 튜터 ID
   */
  public UserProfile(String email, String nickname, Long aiTutorId) {
    this.email = email;
    this.nickname = nickname;
    this.targetLocale = DEFAULT_TARGET_LOCALE;
    this.baseLocale = DEFAULT_BASE_LOCALE;
    this.currentLevel = 1;
    this.aiTutorId = aiTutorId;
    this.accentLocale = AccentLocale.EN_US;
    this.pushPermissionStatus = PushPermissionStatus.NOT_DETERMINED;
    this.subscriptionStatus = SubscriptionStatus.NONE;
    this.status = UserProfileStatus.ACTIVE;
    this.role = UserRole.USER;
  }

  /** 소셜 제공자에서 받은 최신 프로필 정보로 갱신한다. */
  public void updateProfile(String email, String nickname) {
    if (email != null) {
      this.email = email;
    }
    if (nickname != null) {
      this.nickname = nickname;
    }
  }

  /** 온보딩에서 선택한 학습 수준으로 갱신한다. */
  public void updateLearningLevel(int learningLevel) {
    this.learningLevel = learningLevel;
  }

  /**
   * 온보딩에서 선택한 영어 억양으로 갱신한다.
   *
   * @param accentLocale 선택한 영어 억양
   */
  public void updateAccentLocale(AccentLocale accentLocale) {
    this.accentLocale = accentLocale;
  }

  /**
   * 푸시 권한을 허용 상태로 갱신한다.
   *
   * @param updatedAt 권한 상태 갱신 시각
   */
  public void grantPushPermission(LocalDateTime updatedAt) {
    this.pushPermissionStatus = PushPermissionStatus.GRANTED;
    this.pushPermissionUpdatedAt = updatedAt;
  }

  /**
   * 결제 제공자 이벤트로 구독 상태를 갱신한다.
   *
   * @param subscriptionStatus 갱신할 구독 상태
   * @param expiresAt 구독 만료 시각. 알 수 없으면 null
   * @param eventAt 이벤트 발생 시각
   */
  public void updateSubscription(
      SubscriptionStatus subscriptionStatus, LocalDateTime expiresAt, LocalDateTime eventAt) {
    this.subscriptionStatus = subscriptionStatus;
    this.subscriptionExpiresAt = expiresAt;
    this.subscriptionEventAt = eventAt;
  }

  /**
   * 이미 반영한 구독 이벤트보다 오래된 이벤트인지 확인한다.
   *
   * @param eventAt 확인할 이벤트 발생 시각
   * @return 마지막으로 반영한 이벤트보다 이전이면 {@code true}
   */
  public boolean isSubscriptionEventStale(LocalDateTime eventAt) {
    return subscriptionEventAt != null && eventAt.isBefore(subscriptionEventAt);
  }

  /**
   * 프리미엄 혜택이 켜진 사용자인지 확인한다.
   *
   * @return 프리미엄이 켜져 있으면 {@code true}
   */
  public boolean isPremium() {
    return subscriptionStatus.isPremium();
  }

  /** 사용자 프로필을 탈퇴 상태로 전환하고 프로필 이미지를 정리한다. */
  public void withdraw() {
    this.profileImageUrl = null;
    this.status = UserProfileStatus.WITHDRAWN;
  }

  /** 활성 사용자인지 확인한다. */
  public boolean isActive() {
    return status == UserProfileStatus.ACTIVE;
  }
}
