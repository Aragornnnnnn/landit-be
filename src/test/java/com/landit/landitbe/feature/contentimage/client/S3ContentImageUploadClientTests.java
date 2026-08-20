// S3 콘텐츠 이미지 업로드 URL의 서명 계약을 검증한다.

package com.landit.landitbe.feature.contentimage.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.content.ContentImageProperties;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class S3ContentImageUploadClientTests {

  /** S3 PUT URL에 객체 중복 방지와 응답에 안내할 필수 헤더를 모두 서명한다. */
  @Test
  void presignsPutWithRequiredHeadersAndExpiration() {
    ContentImageProperties properties =
        new ContentImageProperties(
            "landit-content-test", URI.create("https://content.example.com"), "ap-northeast-2");
    ContentImageUploadCommand command =
        new ContentImageUploadCommand(
            "content/inbox/7a562408-a649-45c5-95bd-a1fc049c69f4.webp",
            "image/webp",
            "public, max-age=31536000, immutable",
            "*",
            Duration.ofMinutes(5));

    try (S3Presigner presigner = testPresigner()) {
      URI uploadUrl = new S3ContentImageUploadClient(presigner, properties).presign(command);

      assertThat(uploadUrl.getHost())
          .isEqualTo("landit-content-test.s3.ap-northeast-2.amazonaws.com");
      assertThat(uploadUrl.getPath())
          .isEqualTo("/content/inbox/7a562408-a649-45c5-95bd-a1fc049c69f4.webp");
      assertThat(decodedQuery(uploadUrl))
          .contains("X-Amz-Expires=300")
          .contains("X-Amz-SignedHeaders=cache-control;content-type;host;if-none-match");
    }
  }

  /** 응답에서 요구하는 헤더 값과 같은 값으로 S3 PUT 요청을 서명한다. */
  @Test
  void appliesRequiredHeaderValuesToPutRequest() throws Exception {
    S3Presigner presigner = mock(S3Presigner.class);
    PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
    when(presignedRequest.url()).thenReturn(URI.create("https://upload.example.com").toURL());
    when(presigner.presignPutObject(any(PutObjectPresignRequest.class)))
        .thenReturn(presignedRequest);
    ContentImageProperties properties =
        new ContentImageProperties(
            "landit-content-test", URI.create("https://content.example.com"), "ap-northeast-2");
    ContentImageUploadCommand command =
        new ContentImageUploadCommand(
            "content/inbox/image.webp",
            "image/webp",
            "public, max-age=31536000, immutable",
            "*",
            Duration.ofMinutes(5));

    new S3ContentImageUploadClient(presigner, properties).presign(command);

    ArgumentCaptor<PutObjectPresignRequest> captor =
        ArgumentCaptor.forClass(PutObjectPresignRequest.class);
    verify(presigner).presignPutObject(captor.capture());
    assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/webp");
    assertThat(captor.getValue().putObjectRequest().cacheControl())
        .isEqualTo("public, max-age=31536000, immutable");
    assertThat(captor.getValue().putObjectRequest().ifNoneMatch()).isEqualTo("*");
  }

  private S3Presigner testPresigner() {
    return S3Presigner.builder()
        .region(Region.AP_NORTHEAST_2)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
        .build();
  }

  private String decodedQuery(URI uri) {
    return URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);
  }
}
