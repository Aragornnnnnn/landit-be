// OIDC 검증에 필요한 audience와 테스트용 fake verifier 사용 여부를 바인딩한다.

package com.landit.landitbe.config.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OIDC 검증에 필요한 audience와 테스트용 fake verifier 사용 여부를 바인딩한다.
 *
 * @param fakeEnabled 테스트용 OIDC 검증기 사용 여부
 * @param googleAudiences Google OIDC audience 목록
 * @param kakaoAudiences Kakao OIDC audience 목록
 * @param appleAudiences Apple OIDC audience 목록
 */
@ConfigurationProperties(prefix = "landit.auth.oidc")
public record OidcProperties(
    boolean fakeEnabled,
    List<String> googleAudiences,
    List<String> kakaoAudiences,
    List<String> appleAudiences) {

  /** 제공자별 audience 목록에서 공백과 빈 값을 제거한다. */
  public OidcProperties {
    googleAudiences = normalize(googleAudiences);
    kakaoAudiences = normalize(kakaoAudiences);
    appleAudiences = normalize(appleAudiences);
  }

  private static List<String> normalize(List<String> audiences) {
    if (audiences == null) {
      return List.of();
    }
    return audiences.stream().map(String::trim).filter(value -> !value.isBlank()).toList();
  }
}
