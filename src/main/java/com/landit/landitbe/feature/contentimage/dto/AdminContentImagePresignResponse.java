// 관리자 콘텐츠 이미지 업로드 URL 발급 응답을 정의한다.

package com.landit.landitbe.feature.contentimage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * 관리자 콘텐츠 이미지 업로드 URL 발급 응답이다.
 *
 * @param uploadUrl S3 presigned PUT URL
 * @param method 업로드 HTTP method
 * @param headers 업로드 요청에 포함할 서명 헤더
 * @param objectKey 서버가 생성한 S3 객체 키
 * @param imageUrl 업로드 후 조회할 CloudFront URL
 * @param expiresAt presigned URL 만료 시각
 */
@Schema(description = "관리자 콘텐츠 이미지 업로드 URL 발급 응답")
public record AdminContentImagePresignResponse(
    String uploadUrl,
    String method,
    Map<String, String> headers,
    String objectKey,
    String imageUrl,
    Instant expiresAt) {}
