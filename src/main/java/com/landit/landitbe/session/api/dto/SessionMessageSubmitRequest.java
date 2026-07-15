// 사용자 발화 제출 API의 요청 본문을 정의한다.
package com.landit.landitbe.session.api.dto;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.domain.SessionMessageInputType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 발화 제출 요청")
public record SessionMessageSubmitRequest(
    @Schema(description = "사용자 메시지 본문") String content,
    @Schema(description = "입력 타입") SessionMessageInputType inputType) {

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
