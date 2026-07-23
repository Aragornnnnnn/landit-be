// 기능 모듈에서 의도한 실패를 공통 HTTP 응답으로 변환할 최소 계약을 정의한다.

package com.landit.landitbe.shared.exception;

import org.springframework.http.HttpStatus;

/** 기능 모듈에서 의도한 실패를 오류 코드와 HTTP 상태로 표현한다. */
public abstract class FeatureException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  /**
   * 기능 오류 코드와 HTTP 상태, 클라이언트 메시지로 예외를 생성한다.
   *
   * @param code 기능별 오류 코드
   * @param status 응답 HTTP 상태
   * @param message 클라이언트에 노출할 오류 메시지
   */
  protected FeatureException(String code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  /**
   * 기능별 오류 코드 문자열을 반환한다.
   *
   * @return 기능별 오류 코드
   */
  public String getCode() {
    return code;
  }

  /**
   * 오류에 대응하는 HTTP 상태를 반환한다.
   *
   * @return 응답 HTTP 상태
   */
  public HttpStatus getStatus() {
    return status;
  }
}
