// 복습의 만료와 시작 전 접근 오류를 정의한다.

package com.landit.landitbe.feature.learning.review.exception;

import com.landit.landitbe.shared.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** 복습 업무가 소유하는 오류 코드다. */
public enum ReviewErrorCode implements ApiErrorCode {
  REVIEW_EXPIRED(HttpStatus.GONE, "복습 유효기간이 지났습니다."),
  REVIEW_NOT_STARTED(HttpStatus.CONFLICT, "복습을 먼저 시작해 주세요.");

  private final HttpStatus status;
  private final String message;

  ReviewErrorCode(HttpStatus status, String message) {
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
