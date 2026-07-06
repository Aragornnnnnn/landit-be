// OIDC ID Token에서 검증된 사용자 식별 정보만 담는다.
package com.landit.landitbe.auth.application;

import com.landit.landitbe.auth.domain.SocialProvider;

public record OidcUserInfo(
        SocialProvider provider,
        String sub,
        String email,
        String nickname
) {
}
