// 로그인 응답에서 사용자 식별 정보와 신규 가입 여부를 전달한다.
package com.landit.landitbe.auth.api.dto;

public record AuthUserResponse(
        Long userId,
        String nickname,
        String email,
        String provider,
        boolean newUser
) {
}
