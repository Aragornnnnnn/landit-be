// 소셜 로그인을 지원하는 OIDC 제공자를 정의한다.
package com.landit.landitbe.auth.domain;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;

public enum SocialProvider {
    GOOGLE,
    KAKAO,
    APPLE;

    /** 요청 문자열을 지원하는 소셜 제공자로 변환한다. */
    public static SocialProvider from(String value) {
        for (SocialProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new ApiException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
    }
}
