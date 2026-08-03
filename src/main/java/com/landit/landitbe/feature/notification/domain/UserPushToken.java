// 사용자 디바이스의 Expo Push Token을 저장한다.

package com.landit.landitbe.feature.notification.domain;

import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 사용자 디바이스의 Expo Push Token을 저장한다. */
@Entity
@Table(name = "user_push_token")
public class UserPushToken extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppPlatform platform;

  @Column(name = "expo_push_token", nullable = false, length = 500)
  private String expoPushToken;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserPushTokenStatus status;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserPushToken() {}

  // 활성 상태의 Expo Push Token 엔티티를 초기화한다.
  private UserPushToken(Long userProfileId, AppPlatform platform, String expoPushToken) {
    this.userProfileId = userProfileId;
    this.platform = platform;
    this.expoPushToken = expoPushToken;
    this.status = UserPushTokenStatus.ACTIVE;
  }

  /**
   * 활성 상태의 사용자 Expo Push Token을 생성한다.
   *
   * @param userProfileId Token 소유자 사용자 프로필 ID
   * @param platform 앱 플랫폼
   * @param expoPushToken Expo Push Token
   * @return 저장 전 사용자 Expo Push Token
   */
  public static UserPushToken register(
      Long userProfileId, AppPlatform platform, String expoPushToken) {
    return new UserPushToken(userProfileId, platform, expoPushToken);
  }

  /**
   * Token 소유자와 플랫폼을 갱신하고 활성화한다.
   *
   * @param userProfileId Token 소유자 사용자 프로필 ID
   * @param platform 앱 플랫폼
   */
  public void claim(Long userProfileId, AppPlatform platform) {
    this.userProfileId = userProfileId;
    this.platform = platform;
    this.status = UserPushTokenStatus.ACTIVE;
  }

  /** 현재 사용자 소유의 Expo Push Token을 비활성화한다. */
  public void revoke() {
    this.status = UserPushTokenStatus.REVOKED;
  }
}
