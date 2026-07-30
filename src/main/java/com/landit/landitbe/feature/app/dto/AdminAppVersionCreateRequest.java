// 관리자 앱 버전 정책 등록 요청을 검증한다.

package com.landit.landitbe.feature.app.dto;

import com.landit.landitbe.shared.domain.AppPlatform;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 관리자 앱 버전 정책 등록 요청을 검증한다. */
public record AdminAppVersionCreateRequest(
    @NotNull AppPlatform platform,
    @NotBlank @Size(max = 30) String versionName,
    @Min(1) long buildNumber,
    @Min(0) long minimumSupportedBuildNumber,
    @Size(max = 500) String forceUpdateReason,
    @Size(max = 500) String softUpdateReason,
    String releaseNote,
    @NotNull LocalDateTime releasedAt) {}
