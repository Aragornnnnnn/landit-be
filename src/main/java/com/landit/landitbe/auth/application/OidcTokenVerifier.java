// 소셜 제공자의 OIDC ID Token을 검증하고 사용자 정보를 반환한다.
package com.landit.landitbe.auth.application;

import com.landit.landitbe.auth.domain.SocialProvider;

public interface OidcTokenVerifier {

  /** ID Token의 서명, 표준 claim, nonce를 검증한다. */
  OidcUserInfo verify(SocialProvider provider, String idToken, String nonce);
}
