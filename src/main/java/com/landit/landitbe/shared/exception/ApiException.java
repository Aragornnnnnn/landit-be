// 서비스 로직에서 공통 오류 코드로 실패를 표현하는 런타임 예외다.

package com.landit.landitbe.shared.exception;

import org.springframework.http.HttpStatus;

/** 서비스 로직에서 공통 오류 코드로 실패를 표현하는 런타임 예외다. */
public class ApiException extends RuntimeException {

  private final ApiErrorCode errorCode;

  /** 오류 코드의 기본 메시지를 사용하는 API 예외를 생성한다. */
  public ApiException(ApiErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /** 오류 코드와 별도 메시지를 사용하는 API 예외를 생성한다. */
  public ApiException(ApiErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * 외부 호출이나 내부 처리 실패의 원인 체인을 보존한다.
   *
   * @param errorCode 응답 오류 코드
   * @param cause 원인 예외
   * @return 원인이 연결된 API 예외
   */
  public static ApiException causedBy(ApiErrorCode errorCode, Throwable cause) {
    ApiException exception = new ApiException(errorCode);
    exception.initCause(cause);
    return exception;
  }

  /** 예외에 대응하는 오류 코드를 반환한다. */
  public ApiErrorCode getErrorCode() {
    return errorCode;
  }

  /** 예외에 대응하는 HTTP 상태를 반환한다. */
  public HttpStatus getStatus() {
    return errorCode.getStatus();
  }
}
