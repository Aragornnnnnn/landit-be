// refresh token으로 자체 토큰 재발급을 요청한다.
package com.landit.landitbe.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank String refreshToken) {}
