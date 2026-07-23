// refresh token 회전 후 새로 발급된 자체 토큰을 전달한다.

package com.landit.landitbe.feature.auth.api.dto;

/** Refresh token 회전 후 새로 발급된 자체 토큰을 전달한다. */
public record TokenRefreshResponse(
    String tokenType,
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn) {}
