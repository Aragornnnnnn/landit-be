// 관리자 콘텐츠 이미지 업로드 URL 발급 API 계약과 권한을 통합 검증한다.

package com.landit.landitbe.feature.contentimage;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** 관리자 콘텐츠 이미지 업로드 URL 발급 API 계약과 권한을 통합 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AdminContentImageApiIntegrationTests.TestS3PresignerConfiguration.class)
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.content-image.bucket-name=landit-content-test",
      "landit.content-image.cloudfront-url=https://content.example.com",
      "landit.content-image.region=ap-northeast-2"
    })
class AdminContentImageApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 관리자는 UUID 객체 키와 필수 PUT 헤더가 포함된 업로드 정보를 발급받는다. */
  @Test
  void adminCreatesPresignedContentImageUpload() throws Exception {
    String accessToken = loginAdmin("content-image-admin");

    mockMvc
        .perform(
            post("/api/v1/admin/content-images/presigned-url")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fileName":"notice.webp","contentType":"image/webp","fileSize":1842030}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.uploadUrl").value(startsWith("https://")))
        .andExpect(jsonPath("$.data.method").value("PUT"))
        .andExpect(jsonPath("$.data.headers.Content-Type").value("image/webp"))
        .andExpect(
            jsonPath("$.data.headers.Cache-Control").value("public, max-age=31536000, immutable"))
        .andExpect(jsonPath("$.data.headers.If-None-Match").value("*"))
        .andExpect(
            jsonPath("$.data.objectKey")
                .value(matchesPattern("content/inbox/[0-9a-f-]{36}\\.webp")))
        .andExpect(
            jsonPath("$.data.imageUrl")
                .value(
                    matchesPattern(
                        "https://content.example.com/content/inbox/[0-9a-f-]{36}\\.webp")))
        .andExpect(jsonPath("$.data.expiresAt").exists());
  }

  /** 일반 사용자는 관리자 이미지 업로드 URL을 발급받을 수 없다. */
  @Test
  void rejectsNonAdminPresignedContentImageUpload() throws Exception {
    String accessToken = login("content-image-user");

    mockMvc
        .perform(
            post("/api/v1/admin/content-images/presigned-url")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fileName":"notice.webp","contentType":"image/webp","fileSize":1024}
                    """))
        .andExpect(status().isForbidden());
  }

  /** MIME type과 확장자가 다르면 업로드 URL을 발급하지 않는다. */
  @Test
  void rejectsMismatchedContentImageType() throws Exception {
    String accessToken = loginAdmin("content-image-invalid");

    mockMvc
        .perform(
            post("/api/v1/admin/content-images/presigned-url")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fileName":"notice.png","contentType":"image/jpeg","fileSize":1024}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  /** OpenAPI 문서에 관리자 이미지 업로드 URL 발급 계약을 노출한다. */
  @Test
  void documentsPresignedContentImageUpload() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/content-images/presigned-url'].post.summary")
                .exists());
  }

  private String loginAdmin(String userKey) throws Exception {
    String accessToken = login(userKey);
    jdbcTemplate.update(
        "update user_profile set role = 'ADMIN' where email = ?", userKey + "@example.com");
    return accessToken;
  }

  private String login(String userKey) throws Exception {
    String nonce = UUID.randomUUID().toString();
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "provider":"GOOGLE",
                          "idToken":"%s|%s@example.com|Content Admin|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .get("data")
        .get("accessToken")
        .asText();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestS3PresignerConfiguration {

    @Bean
    @Primary
    S3Presigner testContentImageS3Presigner() {
      return S3Presigner.builder()
          .region(Region.AP_NORTHEAST_2)
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create("test-key", "test-secret")))
          .build();
    }
  }
}
