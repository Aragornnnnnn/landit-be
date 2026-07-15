// 자체 access token과 refresh token 발급 설정을 바인딩한다.
package com.landit.landitbe.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "landit.auth.token")
public record TokenProperties(
    String secret, long accessExpiresInSeconds, long refreshExpiresInSeconds) {}
