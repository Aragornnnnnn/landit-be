// 로그인 성공 시 발급된 access token과 refresh token을 전달한다.

package com.landit.landitbe.feature.auth.dto;

/**
 * 로그인 성공 시 발급된 access token과 refresh token을 전달한다.
 *
 * @param tokenType 인증 토큰 유형
 * @param accessToken Access token
 * @param accessTokenExpiresIn Access token 만료 시간(초)
 * @param refreshToken Refresh token
 * @param refreshTokenExpiresIn Refresh token 만료 시간(초)
 * @param user 로그인 사용자 정보
 */
public record AuthTokenResponse(
    String tokenType,
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn,
    AuthUserResponse user) {

  /**
   * 발급된 토큰과 사용자 정보를 로그인 응답으로 변환한다.
   *
   * @param tokenType 인증 토큰 유형
   * @param accessToken Access token
   * @param accessTokenExpiresIn Access token 만료 시간
   * @param refreshToken Refresh token
   * @param refreshTokenExpiresIn Refresh token 만료 시간
   * @param user 로그인 사용자 정보
   * @return 로그인 토큰 응답
   */
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
