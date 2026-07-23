// 자체 access token과 refresh token 발급 설정을 바인딩한다.

package com.landit.landitbe.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 자체 access token과 refresh token 발급 설정을 바인딩한다. */
@ConfigurationProperties(prefix = "landit.auth.token")
public record TokenProperties(
    String secret, long accessExpiresInSeconds, long refreshExpiresInSeconds) {}
