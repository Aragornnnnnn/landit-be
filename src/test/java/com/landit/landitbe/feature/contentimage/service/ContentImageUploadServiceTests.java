// 콘텐츠 이미지 업로드 요청 검증과 응답 생성을 검증한다.

package com.landit.landitbe.feature.contentimage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.content.ContentImageProperties;
import com.landit.landitbe.feature.contentimage.client.ContentImageUploadClient;
import com.landit.landitbe.feature.contentimage.client.ContentImageUploadCommand;
import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignRequest;
import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignResponse;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContentImageUploadServiceTests {

  private static final Instant ISSUED_AT = Instant.parse("2026-08-12T03:05:00Z");
  private static final long TEN_MIB = 10L * 1024 * 1024;

  /** 허용한 MIME type별 표준 확장자로 UUID 객체 키를 발급한다. */
  @ParameterizedTest
  @CsvSource({"photo.jpg,image/jpeg,jpg", "photo.png,image/png,png", "photo.webp,image/webp,webp"})
  void createsUploadForSupportedImageTypes(
      String fileName, String contentType, String expectedExtension) {
    CapturingUploadClient client = new CapturingUploadClient();
    ContentImageUploadService service = service(client);

    AdminContentImagePresignResponse response =
        service.createPresignedUrl(
            new AdminContentImagePresignRequest(fileName, contentType, 1_842_030));

    assertThat(response.uploadUrl()).isEqualTo("https://upload.example.com/signed");
    assertThat(response.method()).isEqualTo("PUT");
    assertThat(response.objectKey()).matches("content/inbox/[0-9a-f-]{36}\\." + expectedExtension);
    assertThat(response.imageUrl())
        .isEqualTo("https://content.example.com/" + response.objectKey());
    assertThat(response.expiresAt()).isEqualTo(ISSUED_AT.plusSeconds(300));
    assertThat(response.headers())
        .containsEntry("Content-Type", contentType)
        .containsEntry("Cache-Control", "public, max-age=31536000, immutable")
        .containsEntry("If-None-Match", "*");
    assertThat(client.command.objectKey()).isEqualTo(response.objectKey());
    assertThat(client.command.expiresIn()).hasSeconds(300);
  }

  /** JPEG의 일반적인 jpeg 확장자는 표준 jpg 객체 키로 정규화한다. */
  @Test
  void normalizesJpegExtensionToJpg() {
    ContentImageUploadService service = service(new CapturingUploadClient());

    AdminContentImagePresignResponse response =
        service.createPresignedUrl(
            new AdminContentImagePresignRequest("photo.jpeg", "image/jpeg", 1024));

    assertThat(response.objectKey()).endsWith(".jpg");
  }

  /** 지원하지 않는 형식과 MIME type이 일치하지 않는 확장자를 거부한다. */
  @ParameterizedTest
  @CsvSource({"photo.gif,image/gif", "photo.png,image/jpeg", "photo,image/png"})
  void rejectsUnsupportedOrMismatchedImageTypes(String fileName, String contentType) {
    ContentImageUploadService service = service(new RejectingUploadClient());

    assertValidationFailure(
        () ->
            service.createPresignedUrl(
                new AdminContentImagePresignRequest(fileName, contentType, 1024)));
  }

  /** 비어 있거나 10 MiB를 초과하는 요청 크기를 거부한다. */
  @ParameterizedTest
  @CsvSource({"0", "-1", "10485761"})
  void rejectsFileSizeOutsideAllowedRange(long fileSize) {
    ContentImageUploadService service = service(new RejectingUploadClient());

    assertValidationFailure(
        () ->
            service.createPresignedUrl(
                new AdminContentImagePresignRequest("photo.webp", "image/webp", fileSize)));
  }

  /** 경계값인 10 MiB 파일은 발급을 허용한다. */
  @Test
  void acceptsTenMibFile() {
    ContentImageUploadService service = service(new CapturingUploadClient());

    AdminContentImagePresignResponse response =
        service.createPresignedUrl(
            new AdminContentImagePresignRequest("photo.webp", "image/webp", TEN_MIB));

    assertThat(response.objectKey()).endsWith(".webp");
  }

  /** 같은 파일 메타데이터로 반복 발급해도 기존 객체를 가리키는 키를 재사용하지 않는다. */
  @Test
  void createsUniqueObjectKeyForEachRequest() {
    ContentImageUploadService service = service(new CapturingUploadClient());
    AdminContentImagePresignRequest request =
        new AdminContentImagePresignRequest("photo.webp", "image/webp", 1024);

    String firstObjectKey = service.createPresignedUrl(request).objectKey();
    String secondObjectKey = service.createPresignedUrl(request).objectKey();

    assertThat(firstObjectKey).isNotEqualTo(secondObjectKey);
  }

  /** URL 발급이 지연돼도 응답 만료 시각은 요청 처리 시작 시점을 기준으로 계산한다. */
  @Test
  void calculatesExpirationFromRequestStart() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(ISSUED_AT, ISSUED_AT.plusSeconds(30));
    ContentImageUploadClient delayedClient =
        command -> {
          clock.instant();
          return URI.create("https://upload.example.com/signed");
        };
    ContentImageProperties properties =
        new ContentImageProperties(
            "landit-content-test", URI.create("https://content.example.com"), "ap-northeast-2");
    ContentImageUploadService service =
        new ContentImageUploadService(delayedClient, properties, clock);

    AdminContentImagePresignResponse response =
        service.createPresignedUrl(
            new AdminContentImagePresignRequest("photo.webp", "image/webp", 1024));

    assertThat(response.expiresAt()).isEqualTo(ISSUED_AT.plusSeconds(300));
  }

  private ContentImageUploadService service(ContentImageUploadClient client) {
    ContentImageProperties properties =
        new ContentImageProperties(
            "landit-content-test", URI.create("https://content.example.com"), "ap-northeast-2");
    Clock clock = Clock.fixed(ISSUED_AT, ZoneOffset.UTC);
    return new ContentImageUploadService(client, properties, clock);
  }

  private void assertValidationFailure(Runnable request) {
    assertThatThrownBy(request::run)
        .isInstanceOf(ApiException.class)
        .extracting(error -> ((ApiException) error).getErrorCode())
        .isEqualTo(ErrorCode.VALIDATION_FAILED);
  }

  private static final class CapturingUploadClient implements ContentImageUploadClient {

    private ContentImageUploadCommand command;

    @Override
    public URI presign(ContentImageUploadCommand command) {
      this.command = command;
      return URI.create("https://upload.example.com/signed");
    }
  }

  private static final class RejectingUploadClient implements ContentImageUploadClient {

    @Override
    public URI presign(ContentImageUploadCommand command) {
      throw new AssertionError("검증에 실패한 요청은 업로드 URL을 발급하면 안 됩니다.");
    }
  }
}
