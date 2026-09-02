// Apple 사용자 이전 REST API 호출 계약을 정의한다.

package com.landit.landitbe.feature.auth.migration;

/** Apple 사용자 이전 REST API 호출 계약이다. */
public interface AppleUserMigrationClient {

  /**
   * 사용자 이전 API에 사용할 access token을 발급받는다.
   *
   * @return Apple access token
   * @throws AppleUserMigrationException Apple 요청이나 응답 검증에 실패할 때
   */
  String requestAccessToken();

  /**
   * 이전 Team 사용자 식별자로 이전 식별자를 발급받는다.
   *
   * @param accessToken 사용자 이전 access token
   * @param providerUserId 이전 Team Apple 사용자 식별자
   * @return Apple 이전 식별자
   * @throws AppleUserMigrationException Apple 요청이나 응답 검증에 실패할 때
   */
  String createTransferSub(String accessToken, String providerUserId);

  /**
   * 이전 식별자를 새 Team 사용자 식별 정보로 교환한다.
   *
   * @param accessToken 사용자 이전 access token
   * @param transferSub Apple 이전 식별자
   * @return 새 Team 사용자 식별 정보
   * @throws AppleUserMigrationException Apple 요청이나 응답 검증에 실패할 때
   */
  AppleRecipientUser exchangeTransferSub(String accessToken, String transferSub);
}
