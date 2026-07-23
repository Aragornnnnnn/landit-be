// 자체 access token과 refresh token 발급 설정을 바인딩한다.

package com.landit.landitbe.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 자체 access token과 refresh token 발급 설정을 바인딩한다.
 *
 * @param secret 자체 토큰 서명 비밀값
 * @param accessExpiresInSeconds Access token 만료 시간(초)
 * @param refreshExpiresInSeconds Refresh token 만료 시간(초)
 */
@ConfigurationProperties(prefix = "landit.auth.token")
public record TokenProperties(
    String secret, long accessExpiresInSeconds, long refreshExpiresInSeconds) {}
