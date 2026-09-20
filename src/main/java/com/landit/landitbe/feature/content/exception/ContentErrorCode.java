// 기능 소유의 오류 코드와 HTTP 상태를 정의한다.

package com.landit.landitbe.feature.content.exception;

import com.landit.landitbe.shared.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** 기능에서 발생하는 오류와 응답 계약이다. */
public enum ContentErrorCode implements ApiErrorCode {
  CATEGORY_LOCKED(HttpStatus.FORBIDDEN, "잠긴 카테고리입니다."),
  SCENARIO_LOCKED(HttpStatus.FORBIDDEN, "잠긴 시나리오입니다."),
  EXPRESSION_LOCKED(HttpStatus.FORBIDDEN, "잠긴 표현입니다."),
  SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "시나리오를 찾을 수 없습니다."),
  PRONUNCIATION_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "발음 분석에 실패했습니다."),
  PRONUNCIATION_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "발음 데이터가 없습니다."),
  INVALID_AUDIO(HttpStatus.BAD_REQUEST, "지원하지 않는 오디오입니다."),
  DEFAULT_AI_TUTOR_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "기본 AI 튜터가 올바르게 설정되지 않았습니다.");
  private final HttpStatus status;
  private final String message;

  ContentErrorCode(HttpStatus status, String message) {
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
