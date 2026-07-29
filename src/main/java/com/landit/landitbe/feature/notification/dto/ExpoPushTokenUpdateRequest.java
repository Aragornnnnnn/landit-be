// Expo Push Token의 등록 상태 변경 요청을 검증한다.

package com.landit.landitbe.feature.notification.dto;

import com.landit.landitbe.shared.domain.AppPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Expo Push Token의 등록 상태 변경 요청을 검증한다. */
public record ExpoPushTokenUpdateRequest(
    @NotNull AppPlatform platform,
    @NotBlank @Size(max = 500) String expoPushToken,
    @NotNull Boolean enabled) {}
