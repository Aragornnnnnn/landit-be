// 사용자 억양 목록 조회와 선택값 저장 API의 인증·계약을 검증한다.

package com.landit.landitbe.feature.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 사용자 억양 목록 조회와 선택값 저장 API의 인증·계약을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AccentLocaleApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 인증된 사용자는 지원 억양 목록을 정해진 순서로 조회한다. */
  @Test
  void listsSupportedAccentLocales() throws Exception {
    String accessToken = login("accent-list");

    mockMvc
        .perform(
            get("/api/v1/accent-locales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].code").value("EN_US"))
        .andExpect(jsonPath("$.data[0].name").value("미국"))
        .andExpect(jsonPath("$.data[1].code").value("EN_GB"))
        .andExpect(jsonPath("$.data[1].name").value("영국"))
        .andExpect(jsonPath("$.data[2].code").value("EN_AU"))
        .andExpect(jsonPath("$.data[2].name").value("호주"));
  }

  /** 신규 사용자는 현재 억양이 미국 영어로 조회된다. */
  @Test
  void returnsDefaultAccentLocaleForNewUser() throws Exception {
    String accessToken = login("accent-default");

    mockMvc
        .perform(
            get("/api/v1/me/accent-locale")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accentLocale").value("EN_US"))
        .andExpect(jsonPath("$.data.name").value("미국"));
  }

  /** 인증된 사용자가 선택한 억양을 저장하고 현재값으로 조회한다. */
  @Test
  void storesAndReadsSelectedAccentLocale() throws Exception {
    String userKey = "accent-owner";
    String accessToken = login(userKey);

    mockMvc
        .perform(
            put("/api/v1/me/accent-locale")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accentLocale\":\"EN_GB\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    mockMvc
        .perform(
            get("/api/v1/me/accent-locale")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accentLocale").value("EN_GB"))
        .andExpect(jsonPath("$.data.name").value("영국"));

    String savedAccentLocale =
        jdbcTemplate.queryForObject(
            "select accent_locale from user_profile where email = ?",
            String.class,
            userKey + "@example.com");
    org.assertj.core.api.Assertions.assertThat(savedAccentLocale).isEqualTo("EN_GB");
  }

  /** 억양이 없거나 지원하지 않는 값이면 요청을 거절한다. */
  @Test
  void rejectsMissingOrUnsupportedAccentLocale() throws Exception {
    String accessToken = login("accent-invalid");

    for (String content : List.of("{}", "{\"accentLocale\":\"EN_KR\"}")) {
      mockMvc
          .perform(
              put("/api/v1/me/accent-locale")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(content))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  /** 인증되지 않은 사용자는 억양 목록과 현재값을 조회하거나 저장할 수 없다. */
  @Test
  void rejectsUnauthenticatedAccentLocaleRequests() throws Exception {
    mockMvc.perform(get("/api/v1/accent-locales")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/me/accent-locale")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            put("/api/v1/me/accent-locale")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accentLocale\":\"EN_US\"}"))
        .andExpect(status().isUnauthorized());
  }

  /** OpenAPI 문서에 억양 목록·현재값 조회·변경 API를 공개한다. */
  @Test
  void openApiDocsDescribeAccentLocaleApis() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/accent-locales'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/accent-locales'].get.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/accent-locale'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/accent-locale'].get.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/accent-locale'].put.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/accent-locale'].put.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/accent-locale'].put.responses['401']").exists());
  }

  /** 테스트 식별자로 가짜 소셜 로그인을 수행하고 access token을 반환한다. */
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
                          "idToken":"%s|%s@example.com|%s|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }
}
