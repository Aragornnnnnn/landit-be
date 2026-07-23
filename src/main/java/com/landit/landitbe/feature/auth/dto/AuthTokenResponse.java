// 로그인 성공 시 발급된 access token과 refresh token을 전달한다.

package com.landit.landitbe.feature.auth.dto;

/** 로그인 성공 시 발급된 access token과 refresh token을 전달한다. */
public record AuthTokenResponse(
    String tokenType,
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn,
    AuthUserResponse user) {

  /** 발급된 토큰과 사용자 정보를 로그인 응답으로 변환한다. */
  public static AuthTokenResponse from(
      String tokenType,
      String accessToken,
      long accessTokenExpiresIn,
      String refreshToken,
      long refreshTokenExpiresIn,
      AuthUserResponse user) {
    return new AuthTokenResponse(
        tokenType, accessToken, accessTokenExpiresIn, refreshToken, refreshTokenExpiresIn, user);
  }
}
