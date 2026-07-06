// 소셜 로그인 요청의 제공자, ID Token, nonce 값을 전달한다.
package com.landit.landitbe.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank
        String provider,

        @NotBlank
        String idToken,

        String nonce
) {
}
