// 공통 예외가 사용할 애플리케이션 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.shared.exception;

import org.springframework.http.HttpStatus;

/** 공통 예외가 사용할 애플리케이션 오류 코드와 HTTP 상태를 정의한다. */
public enum ErrorCode implements ApiErrorCode {
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "요청 기능의 외부 연동 설정을 확인해 주세요."),
  CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
  AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "AI 응답 형식이 올바르지 않습니다."),
  FREE_TALK_CONTEXT_TOO_LARGE(HttpStatus.BAD_REQUEST, "발화가 너무 깁니다. 짧게 나눠 다시 입력해 주세요."),
  FREE_TALK_SUMMARY_INPUT_TOO_LARGE(HttpStatus.BAD_REQUEST, "요약할 대화가 입력 한도를 초과했습니다."),
  AI_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답 생성에 실패했습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  /** 오류 코드에 대응하는 HTTP 상태를 반환한다. */
  public HttpStatus getStatus() {
    return status;
  }

  /** 클라이언트에 노출할 기본 오류 메시지를 반환한다. */
  public String getMessage() {
    return message;
  }
}
