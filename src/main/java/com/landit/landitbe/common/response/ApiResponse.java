// 모든 API 응답을 success, data, error 형태로 감싼다.

package com.landit.landitbe.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 모든 API 응답을 success, data, error 형태로 감싼다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "공통 API 응답 객체")
public record ApiResponse<T>(
    @Schema(description = "요청 처리 성공 여부", example = "true") boolean success,
    @Schema(description = "성공 응답 데이터. 실패 시 null입니다.") T data,
    @Schema(description = "실패 오류 정보. 성공 시 null입니다.") ErrorResponse error) {

  /** 성공 응답 본문을 생성한다. */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null);
  }

  /** 지정한 HTTP 상태의 성공 응답을 생성한다. */
  public static <T> ResponseEntity<ApiResponse<T>> success(HttpStatus status, T data) {
    return ResponseEntity.status(status).body(success(data));
  }

  /** 오류 코드의 기본 메시지로 실패 응답 본문을 생성한다. */
  public static ApiResponse<Void> error(ErrorCode errorCode) {
    return error(errorCode, errorCode.getMessage());
  }

  /** 오류 코드와 별도 메시지로 실패 응답 본문을 생성한다. */
  public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
    return new ApiResponse<>(false, null, new ErrorResponse(errorCode.name(), message));
  }
}
