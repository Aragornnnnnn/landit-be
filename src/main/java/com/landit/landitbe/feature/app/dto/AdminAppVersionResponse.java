// 관리자 앱 버전 정책 조회 결과를 반환한다.

package com.landit.landitbe.feature.app.dto;

import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.shared.domain.AppPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 관리자 앱 버전 정책 조회 결과를 반환한다.
 *
 * @param appVersionId 앱 버전 정책 ID
 * @param platform 앱 플랫폼
 * @param versionName 사용자에게 표시할 버전명
 * @param buildNumber 최신 앱 빌드 번호
 * @param minimumSupportedVersionName 최소 지원 앱 버전명
 * @param forceUpdateReason 강제 업데이트 안내 사유
 * @param softUpdateReason 권장 업데이트 안내 사유
 * @param releaseNote 릴리스 노트
 * @param active 활성 정책 여부
 * @param releasedAt 출시 시각
 * @param updatedAt 마지막 수정 시각
 * @param updatedBy 마지막 수정자 닉네임
 */
public record AdminAppVersionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long appVersionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppPlatform platform,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String versionName,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long buildNumber,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String minimumSupportedVersionName,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"string", "null"})
        String forceUpdateReason,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"string", "null"})
        String softUpdateReason,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"string", "null"})
        String releaseNote,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean active,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"string", "null"})
        LocalDateTime releasedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updatedAt,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"string", "null"})
        String updatedBy) {

  /**
   * 앱 버전 정책 엔티티를 관리자 응답으로 변환한다.
   *
   * @param appVersion 변환할 앱 버전 정책
   * @param updatedBy 마지막 수정자 닉네임
   * @return 관리자 앱 버전 정책 응답
   */
  public static AdminAppVersionResponse from(AppVersion appVersion, String updatedBy) {
    return new AdminAppVersionResponse(
        appVersion.getId(),
        appVersion.getPlatform(),
        appVersion.getVersionName(),
        appVersion.getBuildNumber(),
        appVersion.getMinimumSupportedVersionName(),
        appVersion.getForceUpdateReason(),
        appVersion.getSoftUpdateReason(),
        appVersion.getReleaseNote(),
        appVersion.isActive(),
        appVersion.getReleasedAt(),
        appVersion.getUpdatedAt(),
        updatedBy);
  }
}
