// 관리자 콘텐츠 이미지 업로드 URL 발급 요청을 정의한다.

package com.landit.landitbe.feature.contentimage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 관리자 콘텐츠 이미지 업로드 URL 발급 요청이다.
 *
 * @param fileName 확장자 검증에 사용할 원본 파일명
 * @param contentType 이미지 MIME type
 * @param fileSize 클라이언트가 제출한 파일 크기
 */
@Schema(description = "관리자 콘텐츠 이미지 업로드 URL 발급 요청")
public record AdminContentImagePresignRequest(
    @NotBlank @Schema(description = "원본 파일명", example = "notice-image.webp") String fileName,
    @NotBlank @Schema(description = "이미지 MIME type", example = "image/webp") String contentType,
    @Positive @Max(10 * 1024 * 1024) @Schema(description = "파일 크기(byte)", example = "1842030")
        long fileSize) {}
