// 로그아웃할 refresh token을 전달한다.
package com.landit.landitbe.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank
        String refreshToken
) {
}
