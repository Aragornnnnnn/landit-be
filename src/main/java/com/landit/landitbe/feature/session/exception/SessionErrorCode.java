// 세션 기능에서 예상 가능한 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.feature.session.exception;

import org.springframework.http.HttpStatus;

/** 세션 기능에서 예상 가능한 오류 코드와 HTTP 상태를 정의한다. */
public enum SessionErrorCode {
  SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 세션입니다."),
  SESSION_NOT_COMPLETED(HttpStatus.CONFLICT, "완료되지 않은 세션입니다."),

  /** KST 당일 사용자 발화 시간을 모두 사용한 경우다. */
  FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "오늘의 프리톡 발화 시간을 모두 사용했습니다."),

  /** 계정별 일일 생성 요청 상한에 도달한 경우다. */
  FREE_TALK_DAILY_REQUEST_LIMIT_EXCEEDED(
      HttpStatus.TOO_MANY_REQUESTS, "오늘의 프리톡 요청 한도에 도달했습니다. 내일 다시 이용해 주세요."),

  /** 계정별 고정 1분 구간의 생성 요청 상한에 도달한 경우다. */
  FREE_TALK_REQUEST_RATE_LIMIT_EXCEEDED(
      HttpStatus.TOO_MANY_REQUESTS, "프리톡 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");

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
