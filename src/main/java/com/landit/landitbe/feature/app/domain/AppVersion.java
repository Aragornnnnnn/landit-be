// 앱 버전별 업데이트 정책을 저장한다.

package com.landit.landitbe.feature.app.domain;

import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 앱 버전별 업데이트 정책을 저장한다. */
@Entity
@Table(name = "app_version")
public class AppVersion extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppPlatform platform;

  @Column(name = "version_name", nullable = false, length = 30)
  private String versionName;

  @Column(name = "build_number", nullable = false)
  private long buildNumber;

  @Column(name = "minimum_supported_version_name", nullable = false, length = 30)
  private String minimumSupportedVersionName;

  @Column(name = "force_update_reason", length = 500)
  private String forceUpdateReason;

  @Column(name = "soft_update_reason", length = 500)
  private String softUpdateReason;

  @Column(name = "release_note", columnDefinition = "text")
  private String releaseNote;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "released_at")
  private LocalDateTime releasedAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected AppVersion() {}

  /**
   * 관리자 입력값으로 앱 버전 정책의 표시·업데이트 기준을 변경한다.
   *
   * @param versionName 사용자에게 표시할 버전명
   * @param buildNumber 최신 앱 빌드 번호
   * @param minimumSupportedVersionName 최소 지원 앱 버전명
   * @param forceUpdateReason 강제 업데이트 안내 사유
   * @param softUpdateReason 권장 업데이트 안내 사유
   * @param releaseNote 릴리스 노트
   * @param releasedAt 출시 시각
   */
  public void update(
      String versionName,
      long buildNumber,
      String minimumSupportedVersionName,
      String forceUpdateReason,
      String softUpdateReason,
      String releaseNote,
      LocalDateTime releasedAt) {
    this.versionName = versionName;
    this.buildNumber = buildNumber;
    this.minimumSupportedVersionName = minimumSupportedVersionName;
    this.forceUpdateReason = forceUpdateReason;
    this.softUpdateReason = softUpdateReason;
    this.releaseNote = releaseNote;
    this.releasedAt = releasedAt;
  }

  /**
   * 정책 ID를 반환한다.
   *
   * @return 앱 버전 정책 ID
   */
  public Long getId() {
    return id;
  }

  /**
   * 앱 플랫폼을 반환한다.
   *
   * @return 앱 플랫폼
   */
  public AppPlatform getPlatform() {
    return platform;
  }

  /**
   * 릴리스 노트를 반환한다.
   *
   * @return 릴리스 노트
   */
  public String getReleaseNote() {
    return releaseNote;
  }

  /**
   * 활성 정책 여부를 반환한다.
   *
   * @return 활성 정책이면 {@code true}
   */
  public boolean isActive() {
    return active;
  }

  /**
   * 최신 앱 버전명을 반환한다.
   *
   * @return 사용자에게 표시할 버전명
   */
  public String getVersionName() {
    return versionName;
  }

  /**
   * 최신 앱 빌드 번호를 반환한다.
   *
   * @return 최신 앱 빌드 번호
   */
  public long getBuildNumber() {
    return buildNumber;
  }

  /**
   * 서버가 허용하는 최소 앱 버전명을 반환한다.
   *
   * @return 최소 지원 앱 버전명
   */
  public String getMinimumSupportedVersionName() {
    return minimumSupportedVersionName;
  }

  /**
   * 강제 업데이트 안내 사유를 반환한다.
   *
   * @return 강제 업데이트 안내 사유
   */
  public String getForceUpdateReason() {
    return forceUpdateReason;
  }

  /**
   * 권장 업데이트 안내 사유를 반환한다.
   *
   * @return 권장 업데이트 안내 사유
   */
  public String getSoftUpdateReason() {
    return softUpdateReason;
  }

  /**
   * 최신 버전 릴리스 시각을 반환한다.
   *
   * @return 출시 시각
   */
  public LocalDateTime getReleasedAt() {
    return releasedAt;
  }
}
