// 관리자 앱 버전 정책 조회 결과를 반환한다.

package com.landit.landitbe.feature.app.dto;

import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.time.LocalDateTime;

/** 관리자 앱 버전 정책 조회 결과를 반환한다. */
public record AdminAppVersionResponse(
    Long appVersionId,
    AppPlatform platform,
    String versionName,
    long buildNumber,
    long minimumSupportedBuildNumber,
    String forceUpdateReason,
    String softUpdateReason,
    String releaseNote,
    boolean active,
    LocalDateTime releasedAt) {

  /** 앱 버전 정책 엔티티를 관리자 응답으로 변환한다. */
  public static AdminAppVersionResponse from(AppVersion appVersion) {
    return new AdminAppVersionResponse(
        appVersion.getId(),
        appVersion.getPlatform(),
        appVersion.getVersionName(),
        appVersion.getBuildNumber(),
        appVersion.getMinimumSupportedBuildNumber(),
        appVersion.getForceUpdateReason(),
        appVersion.getSoftUpdateReason(),
        appVersion.getReleaseNote(),
        appVersion.isActive(),
        appVersion.getReleasedAt());
  }
}
