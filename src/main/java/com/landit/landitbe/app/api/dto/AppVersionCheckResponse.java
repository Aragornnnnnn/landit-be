// 앱 버전 확인 결과와 업데이트 필요 수준을 정의한다.
package com.landit.landitbe.app.api.dto;

import java.time.LocalDateTime;

public record AppVersionCheckResponse(
    UpdateType updateType,
    String latestVersionName,
    long latestBuildNumber,
    long minimumSupportedBuildNumber,
    String reason,
    LocalDateTime releasedAt) {

  public enum UpdateType {
    FORCE,
    SOFT,
    NONE
  }
}
