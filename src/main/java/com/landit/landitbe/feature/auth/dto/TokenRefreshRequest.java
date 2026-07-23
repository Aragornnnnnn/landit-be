// refresh token으로 자체 토큰 재발급을 요청한다.

package com.landit.landitbe.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Refresh token으로 자체 토큰 재발급을 요청한다. */
public record TokenRefreshRequest(@NotBlank String refreshToken) {}
