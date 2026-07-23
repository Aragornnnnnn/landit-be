// 앱 버전 확인 결과와 업데이트 필요 수준을 정의한다.

package com.landit.landitbe.feature.app.dto;

import com.landit.landitbe.feature.app.domain.AppVersion;
import java.time.LocalDateTime;

/** 앱 버전 확인 결과와 업데이트 필요 수준을 정의한다. */
public record AppVersionCheckResponse(
    UpdateType updateType,
    String latestVersionName,
    long latestBuildNumber,
    long minimumSupportedBuildNumber,
    String reason,
    LocalDateTime releasedAt) {

  /** 앱 버전 정책과 계산된 업데이트 수준을 API 응답으로 변환한다. */
  public static AppVersionCheckResponse from(
      AppVersion policy, UpdateType updateType, String reason) {
    return new AppVersionCheckResponse(
        updateType,
        policy.getVersionName(),
        policy.getBuildNumber(),
        policy.getMinimumSupportedBuildNumber(),
        reason,
        policy.getReleasedAt());
  }

  /** 내부 타입을 정의한다. */
  public enum UpdateType {
    FORCE,
    SOFT,
    NONE
  }
}
