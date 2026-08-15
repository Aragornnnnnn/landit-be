// 관리자 콘텐츠 이미지 업로드 요청을 검증하고 발급 정보를 생성한다.

package com.landit.landitbe.feature.contentimage.service;

import com.landit.landitbe.config.content.ContentImageProperties;
import com.landit.landitbe.feature.contentimage.client.ContentImageUploadClient;
import com.landit.landitbe.feature.contentimage.client.ContentImageUploadCommand;
import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignRequest;
import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignResponse;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** 관리자 콘텐츠 이미지 업로드 요청을 검증하고 presigned URL을 발급한다. */
@Service
public class ContentImageUploadService {

  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
  private static final Duration URL_EXPIRATION = Duration.ofMinutes(5);
  private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
  private static final String IF_NONE_MATCH = "*";

  private final ContentImageUploadClient uploadClient;
  private final ContentImageProperties properties;
  private final Clock clock;

  /**
   * 이미지 업로드 Client와 런타임 설정, 기준 시각을 주입받는다.
   *
   * @param uploadClient 이미지 업로드 URL 발급 Client
   * @param properties 콘텐츠 이미지 런타임 설정
   * @param clock URL 만료 시각 계산 기준
   */
  public ContentImageUploadService(
      ContentImageUploadClient uploadClient, ContentImageProperties properties, Clock clock) {
    this.uploadClient = uploadClient;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * 파일 메타데이터를 검증하고 S3 업로드와 CloudFront 조회 정보를 발급한다.
   *
   * @param request 이미지 파일 메타데이터
   * @return presigned PUT URL과 업로드 조건
   * @throws ApiException 지원하지 않는 형식이거나 요청 크기가 허용 범위를 벗어날 때
   */
  public AdminContentImagePresignResponse createPresignedUrl(
      AdminContentImagePresignRequest request) {
    validateFileSize(request.fileSize());
    String extension = resolveExtension(request.fileName(), request.contentType());
    String objectKey = "content/inbox/%s.%s".formatted(UUID.randomUUID(), extension);
    ContentImageUploadCommand command = uploadCommand(objectKey, request.contentType());
    Instant expiresAt = Instant.now(clock).plus(URL_EXPIRATION);
    URI uploadUrl = uploadClient.presign(command);

    return new AdminContentImagePresignResponse(
        uploadUrl.toString(),
        "PUT",
        uploadHeaders(request.contentType()),
        objectKey,
        imageUrl(objectKey),
        expiresAt);
  }

  private void validateFileSize(long fileSize) {
    if (fileSize < 1 || fileSize > MAX_FILE_SIZE) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "이미지는 10 MiB 이하만 업로드할 수 있습니다.");
    }
  }

  private String resolveExtension(String fileName, String contentType) {
    if (fileName == null || contentType == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    String fileExtension = extractExtension(fileName);
    String canonicalExtension = canonicalExtension(contentType);
    boolean jpegExtension = contentType.equals("image/jpeg") && fileExtension.equals("jpeg");
    if (!fileExtension.equals(canonicalExtension) && !jpegExtension) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "파일 확장자와 MIME type이 일치하지 않습니다.");
    }
    return canonicalExtension;
  }

  private String extractExtension(String fileName) {
    int extensionSeparator = fileName.lastIndexOf('.');
    if (extensionSeparator < 0 || extensionSeparator == fileName.length() - 1) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "파일 확장자가 필요합니다.");
    }
    return fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
  }

  private String canonicalExtension(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default -> throw new ApiException(ErrorCode.VALIDATION_FAILED, "지원하지 않는 이미지 형식입니다.");
    };
  }

  private ContentImageUploadCommand uploadCommand(String objectKey, String contentType) {
    return new ContentImageUploadCommand(
        objectKey, contentType, CACHE_CONTROL, IF_NONE_MATCH, URL_EXPIRATION);
  }

  private Map<String, String> uploadHeaders(String contentType) {
    return Map.of(
        "Content-Type", contentType,
        "Cache-Control", CACHE_CONTROL,
        "If-None-Match", IF_NONE_MATCH);
  }

  private String imageUrl(String objectKey) {
    String cloudfrontUrl = properties.cloudfrontUrl().toString();
    String separator = cloudfrontUrl.endsWith("/") ? "" : "/";
    return cloudfrontUrl + separator + objectKey;
  }
}
