// 서비스 사용자 프로필과 학습 기본 설정을 저장한다.

package com.landit.landitbe.feature.profile.domain;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "learning_level", length = 20)
  private LearningLevel learningLevel;

  @Column(name = "current_level", nullable = false)
  private int currentLevel;

  @Column(name = "ai_tutor_id")
  private Long aiTutorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "push_permission_status", nullable = false, length = 30)
  private PushPermissionStatus pushPermissionStatus;

  @Column(name = "push_permission_updated_at")
  private LocalDateTime pushPermissionUpdatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserProfileStatus status;

  /** 동작을 수행한다. */
  protected UserProfile() {}

  /** 동작을 수행한다. */
  public UserProfile(String email, String nickname, Long aiTutorId) {
    this.email = email;
    this.nickname = nickname;
    this.targetLocale = DEFAULT_TARGET_LOCALE;
    this.baseLocale = DEFAULT_BASE_LOCALE;
    this.currentLevel = 1;
    this.aiTutorId = aiTutorId;
    this.pushPermissionStatus = PushPermissionStatus.NOT_DETERMINED;
    this.status = UserProfileStatus.ACTIVE;
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
