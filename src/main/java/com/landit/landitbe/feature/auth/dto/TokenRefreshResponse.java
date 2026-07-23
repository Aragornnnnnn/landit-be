// refresh token 회전 후 새로 발급된 자체 토큰을 전달한다.

package com.landit.landitbe.feature.auth.dto;

/** Refresh token 회전 후 새로 발급된 자체 토큰을 전달한다. */
public record TokenRefreshResponse(
    String tokenType,
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn) {

  /** 회전 발급한 토큰 값을 갱신 응답으로 변환한다. */
  public static TokenRefreshResponse from(
      String tokenType,
      String accessToken,
      long accessTokenExpiresIn,
      String refreshToken,
      long refreshTokenExpiresIn) {
    return new TokenRefreshResponse(
        tokenType, accessToken, accessTokenExpiresIn, refreshToken, refreshTokenExpiresIn);
  }
}
