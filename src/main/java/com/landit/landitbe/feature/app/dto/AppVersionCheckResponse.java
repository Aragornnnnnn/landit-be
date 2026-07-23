// 앱 버전 확인 결과와 업데이트 필요 수준을 정의한다.

package com.landit.landitbe.feature.app.dto;

import com.landit.landitbe.feature.app.domain.AppVersion;
import java.time.LocalDateTime;

/**
 * 앱 버전 확인 결과와 업데이트 필요 수준을 정의한다.
 *
 * @param updateType 앱 업데이트 유형
 * @param latestVersionName 최신 앱 버전명
 * @param latestBuildNumber 최신 빌드 번호
 * @param minimumSupportedBuildNumber 최소 지원 빌드 번호
 * @param reason 상태 변경 사유
 * @param releasedAt 앱 버전 출시 시각
 */
public record AppVersionCheckResponse(
    UpdateType updateType,
    String latestVersionName,
    long latestBuildNumber,
    long minimumSupportedBuildNumber,
    String reason,
    LocalDateTime releasedAt) {

  /**
   * 앱 버전 정책과 계산된 업데이트 수준을 API 응답으로 변환한다.
   *
   * @param policy 활성 앱 버전 정책
   * @param updateType 계산된 업데이트 유형
   * @param reason 업데이트 판단 사유
   * @return 앱 버전 확인 응답
   */
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

  /** 앱 업데이트가 필수인지, 선택인지, 불필요한지를 구분한다. */
  public enum UpdateType {
    FORCE,
    SOFT,
    NONE
  }
}
