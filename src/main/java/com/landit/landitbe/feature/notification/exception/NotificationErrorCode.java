// 기능 소유의 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.feature.notification.exception;

import com.landit.landitbe.shared.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** 기능에서 발생하는 오류와 응답 계약이다. */
public enum NotificationErrorCode implements ApiErrorCode {
  IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "같은 요청 키에 다른 입력을 사용할 수 없습니다."),
  PUSH_PAYLOAD_TOO_LARGE(HttpStatus.BAD_REQUEST, "푸시 메시지가 허용 크기를 초과했습니다.");
  private final HttpStatus status;
  private final String message;

  NotificationErrorCode(HttpStatus status, String message) {
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
