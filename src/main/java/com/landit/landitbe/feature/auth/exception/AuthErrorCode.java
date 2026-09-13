// 기능 소유의 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.feature.auth.exception;

import com.landit.landitbe.shared.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** 기능에서 발생하는 오류와 응답 계약이다. */
public enum AuthErrorCode implements ApiErrorCode {
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
  ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료됐습니다."),
  REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "재발급 토큰이 올바르지 않습니다."),
  UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
  OIDC_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "소셜 로그인 토큰이 올바르지 않습니다."),
  OIDC_NONCE_MISMATCH(HttpStatus.BAD_REQUEST, "소셜 로그인 요청 검증 값이 일치하지 않습니다."),
  OIDC_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 제공자 검증에 실패했습니다.");
  private final HttpStatus status;
  private final String message;

  AuthErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  /** {@inheritDoc} */
  @Override
  public HttpStatus getStatus() {
    return status;
  }

  /** {@inheritDoc} */
  @Override
  public String getMessage() {
    return message;
  }
}
