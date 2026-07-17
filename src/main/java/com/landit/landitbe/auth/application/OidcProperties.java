// OIDC 검증에 필요한 audience와 테스트용 fake verifier 사용 여부를 바인딩한다.

package com.landit.landitbe.auth.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** OIDC 검증에 필요한 audience와 테스트용 fake verifier 사용 여부를 바인딩한다. */
@ConfigurationProperties(prefix = "landit.auth.oidc")
public record OidcProperties(
    boolean fakeEnabled,
    List<String> googleAudiences,
    List<String> kakaoAudiences,
    List<String> appleAudiences) {

  /** 동작을 수행한다. */
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
