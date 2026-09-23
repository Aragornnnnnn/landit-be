// 기능별 오류 코드의 공통 HTTP 응답 계약을 정의한다.

package com.landit.landitbe.shared.exception;

import org.springframework.http.HttpStatus;

/** 기능 오류를 공통 HTTP 응답으로 표현하기 위한 최소 계약이다. */
public interface ApiErrorCode {
  /**
   * 외부에 노출할 오류 코드다.
   *
   * @return 오류 코드 문자열
   */
  String name();

  /**
   * 응답 상태를 반환한다.
   *
   * @return HTTP 상태
   */
  HttpStatus getStatus();

  /**
   * 기본 오류 메시지를 반환한다.
   *
   * @return 클라이언트 메시지
   */
  String getMessage();
}
