// 세션 기능에서 예상 가능한 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.feature.session.exception;

import org.springframework.http.HttpStatus;

/** 세션 기능에서 예상 가능한 오류 코드와 HTTP 상태를 정의한다. */
public enum SessionErrorCode {
  SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 세션입니다."),
  SESSION_NOT_COMPLETED(HttpStatus.CONFLICT, "완료되지 않은 세션입니다.");

  private final HttpStatus status;
  private final String message;

  SessionErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  /**
   * 오류 코드에 대응하는 HTTP 상태를 반환한다.
   *
   * @return 응답 HTTP 상태
   */
  public HttpStatus getStatus() {
    return status;
  }

  /**
   * 클라이언트에 노출할 기본 오류 메시지를 반환한다.
   *
   * @return 기본 오류 메시지
   */
  public String getMessage() {
    return message;
  }
}
