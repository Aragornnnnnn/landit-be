// 테스트에서 외부 OIDC 제공자 호출 없이 ID Token 검증 결과를 만든다.

package com.landit.landitbe.feature.auth.application;

import com.landit.landitbe.feature.auth.domain.SocialProvider;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 테스트에서 외부 OIDC 제공자 호출 없이 ID Token 검증 결과를 만든다. */
@Component
@ConditionalOnProperty(prefix = "landit.auth.oidc", name = "fake-enabled", havingValue = "true")
public class FakeOidcTokenVerifier implements OidcTokenVerifier {

  private static final int TOKEN_PART_COUNT = 4;

  /** Fake ID Token 형식의 nonce와 요청 nonce를 비교한다. */
  @Override
  public OidcUserInfo verify(SocialProvider provider, String idToken, String nonce) {
    String[] parts = idToken.split("\\|", -1);
    if (parts.length != TOKEN_PART_COUNT || parts[0].isBlank() || parts[3].isBlank()) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
    verifyNonce(parts[3], nonce);
    return new OidcUserInfo(
        provider, parts[0], blankToNull(parts[1]), nickname(parts[2], parts[0]));
  }

  private void verifyNonce(String tokenNonce, String requestNonce) {
    if (requestNonce == null || requestNonce.isBlank()) {
      throw new ApiException(ErrorCode.OIDC_NONCE_MISMATCH);
    }

    byte[] expected = tokenNonce.getBytes(StandardCharsets.UTF_8);
    byte[] actual = requestNonce.getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(expected, actual)) {
      throw new ApiException(ErrorCode.OIDC_NONCE_MISMATCH);
    }
  }

  private String blankToNull(String value) {
    return value.isBlank() ? null : value;
  }

  private String nickname(String value, String fallback) {
    if (value.isBlank()) {
      return fallback;
    }
    return value;
  }
}
