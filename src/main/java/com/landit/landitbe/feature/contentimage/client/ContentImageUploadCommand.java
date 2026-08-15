// 콘텐츠 이미지 업로드 URL에 서명할 조건을 전달한다.

package com.landit.landitbe.feature.contentimage.client;

import java.time.Duration;

/**
 * 콘텐츠 이미지 업로드 URL에 서명할 조건이다.
 *
 * @param objectKey S3 객체 키
 * @param contentType 이미지 MIME type
 * @param cacheControl 이미지 캐시 정책
 * @param ifNoneMatch 기존 객체 덮어쓰기 방지 조건
 * @param expiresIn URL 유효 시간
 */
public record ContentImageUploadCommand(
    String objectKey,
    String contentType,
    String cacheControl,
    String ifNoneMatch,
    Duration expiresIn) {}
