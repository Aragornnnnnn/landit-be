// 관리자 앱 버전 정책 수정 요청을 검증한다.

package com.landit.landitbe.feature.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 관리자 앱 버전 정책 수정 요청을 검증한다.
 *
 * @param versionName 사용자에게 표시할 버전명
 * @param buildNumber 최신 앱 빌드 번호
 * @param minimumSupportedVersionName 최소 지원 앱 버전명
 * @param forceUpdateReason 강제 업데이트 안내 사유
 * @param softUpdateReason 권장 업데이트 안내 사유
 * @param releaseNote 릴리스 노트
 * @param releasedAt 출시 시각
 */
public record AdminAppVersionUpdateRequest(
    @NotBlank @Pattern(regexp = "\\d+\\.\\d+\\.\\d+") @Size(max = 30) String versionName,
    @Min(1) long buildNumber,
    @NotBlank @Pattern(regexp = "\\d+\\.\\d+\\.\\d+") @Size(max = 30)
        String minimumSupportedVersionName,
    @Size(max = 500) String forceUpdateReason,
    @Size(max = 500) String softUpdateReason,
    String releaseNote,
    @NotNull LocalDateTime releasedAt) {}
