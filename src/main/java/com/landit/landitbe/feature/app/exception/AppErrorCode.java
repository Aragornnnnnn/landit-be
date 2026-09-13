// 기능 소유의 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.feature.app.exception;

import com.landit.landitbe.shared.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** 기능에서 발생하는 오류와 응답 계약이다. */
public enum AppErrorCode implements ApiErrorCode {
  APP_VERSION_POLICY_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "앱 버전 정책이 올바르게 설정되지 않았습니다.");
  private final HttpStatus status;
  private final String message;

  AppErrorCode(HttpStatus status, String message) {
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
