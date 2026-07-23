// 로그인 성공 시 발급된 access token과 refresh token을 전달한다.

package com.landit.landitbe.feature.auth.api.dto;

/** 로그인 성공 시 발급된 access token과 refresh token을 전달한다. */
public record AuthTokenResponse(
    String tokenType,
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn,
    AuthUserResponse user) {}
