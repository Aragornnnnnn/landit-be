// 앱 설치별 푸시 알림 설정과 Expo Token을 저장한다.

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
import java.util.UUID;
import lombok.Getter;

/** 앱 설치별 푸시 알림 설정과 Expo Token을 저장한다. */
@Getter
@Entity
@Table(name = "user_push_token")
public class PushDevice extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "installation_id", nullable = false, unique = true)
  private UUID installationId;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppPlatform platform;

  @Column(name = "push_enabled", nullable = false)
  private boolean pushEnabled;

  @Column(name = "token", length = 500, unique = true)
  private String expoPushToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20)
  private PushTokenStatus tokenStatus;

  /** JPA에서 사용하는 기본 생성자다. */
  protected PushDevice() {}

  /** 최초 설치 상태를 받아 Push Device를 생성한다. */
  private PushDevice(
      Long userProfileId,
      UUID installationId,
      AppPlatform platform,
      boolean pushEnabled,
      String expoPushToken) {
    this.userProfileId = userProfileId;
    this.installationId = installationId;
    synchronize(userProfileId, platform, pushEnabled, expoPushToken);
  }

  /** 현재 설치 상태로 Push Device를 생성한다. */
  public static PushDevice create(
      Long userProfileId,
      UUID installationId,
      AppPlatform platform,
      boolean pushEnabled,
      String expoPushToken) {
    return new PushDevice(userProfileId, installationId, platform, pushEnabled, expoPushToken);
  }

  /** 현재 인증 사용자와 앱이 전달한 설치 상태로 갱신한다. */
  public void synchronize(
      Long userProfileId, AppPlatform platform, boolean pushEnabled, String expoPushToken) {
    this.userProfileId = userProfileId;
    this.platform = platform;
    this.pushEnabled = pushEnabled;
    this.expoPushToken = expoPushToken;
    this.tokenStatus = expoPushToken == null ? null : PushTokenStatus.ACTIVE;
  }

  /** 현재 Expo Token 연결을 해제한다. */
  public void detachToken() {
    expoPushToken = null;
    tokenStatus = null;
  }

  /** Expo가 사용할 수 없다고 응답한 Token을 무효화한다. */
  public void invalidateToken() {
    if (expoPushToken != null) {
      tokenStatus = PushTokenStatus.INVALID;
    }
  }

  /** 현재 설치가 푸시 발송 조건을 만족하는지 반환한다. */
  public boolean isSendable() {
    return pushEnabled && expoPushToken != null && tokenStatus == PushTokenStatus.ACTIVE;
  }
}
