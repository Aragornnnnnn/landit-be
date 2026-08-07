// 공통 오류 응답의 code와 message를 표현한다.

package com.landit.landitbe.shared.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 공통 오류 응답의 code와 message를 표현한다.
 *
 * @param code 애플리케이션 오류 코드
 * @param message 오류 메시지
 */
@Schema(description = "공통 오류 응답 객체")
public record ErrorResponse(
    @Schema(description = "애플리케이션 오류 코드", example = "RESOURCE_NOT_FOUND") String code,
    @Schema(description = "오류 메시지", example = "요청한 리소스를 찾을 수 없습니다.") String message) {}
