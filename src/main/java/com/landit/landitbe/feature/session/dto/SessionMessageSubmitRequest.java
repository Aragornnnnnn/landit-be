// 사용자 발화 제출 API의 요청 본문을 정의한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자 발화 제출 API의 요청 본문을 정의한다.
 *
 * @param content 사용자 메시지 본문
 * @param inputType 입력 타입
 */
@Schema(description = "사용자 발화 제출 요청")
public record SessionMessageSubmitRequest(
    @Schema(description = "사용자 메시지 본문") String content,
    @Schema(description = "입력 타입") SessionMessageInputType inputType,
    @Schema(description = "재전송 시 유지할 클라이언트 메시지 UUID. 구버전 요청은 생략 가능") String clientMessageId) {

  /** 구버전 요청은 클라이언트 메시지 식별자가 없다. */
  public SessionMessageSubmitRequest(String content, SessionMessageInputType inputType) {
    this(content, inputType, null);
  }

  /** 제공된 멱등성 키를 정규화하고 형식을 확인한다. */
  public String validatedClientMessageId() {
    if (clientMessageId == null) {
      return null;
    }
    try {
      String normalized = java.util.UUID.fromString(clientMessageId).toString();
      if (!normalized.equalsIgnoreCase(clientMessageId)) {
        throw new IllegalArgumentException();
      }
      return normalized;
    } catch (IllegalArgumentException exception) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
  }

  /** 공백을 제거한 메시지 본문을 반환한다. */
  public String normalizedContent() {
    if (content == null || content.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    return content.trim();
  }

  /** 필수 입력 타입을 반환한다. */
  public SessionMessageInputType requiredInputType() {
    if (inputType == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    return inputType;
  }
}
