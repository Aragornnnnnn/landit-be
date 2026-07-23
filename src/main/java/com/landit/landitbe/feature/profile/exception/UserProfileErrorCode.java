// 사용자 프로필 기능에서 예상 가능한 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.feature.profile.exception;

import org.springframework.http.HttpStatus;

/** 사용자 프로필 기능에서 예상 가능한 오류 코드와 HTTP 상태를 정의한다. */
public enum UserProfileErrorCode {
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");

  private final HttpStatus status;
  private final String message;

  UserProfileErrorCode(HttpStatus status, String message) {
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
