// 관리자 허용 목록에 따른 API 접근 제어를 통합 검증한다.

package com.landit.landitbe.feature.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** 관리자 허용 목록에 따른 API 접근 제어를 통합 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminAuthorizationIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 관리자 허용 목록에 없는 일반 로그인 사용자는 관리자 경로에 접근할 수 없다. */
  @Test
  void rejectsAuthenticatedNonAdminFromAdminPath() throws Exception {
    String accessToken = login("admin-access-denied");

    mockMvc
        .perform(
            get("/api/v1/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  /** 관리자 허용 목록에 없는 일반 로그인 사용자는 정확한 관리자 루트에도 접근할 수 없다. */
  @Test
  void rejectsAuthenticatedNonAdminFromExactAdminRootPath() throws Exception {
    String accessToken = login("admin-root-access-denied");

    mockMvc
        .perform(get("/api/v1/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  /** 관리자 허용 목록에 등록된 사용자는 관리자 경로의 다음 처리 단계까지 도달한다. */
  @Test
  void allowsRegisteredAdminToReachAdminPath() throws Exception {
    String userKey = "admin-access-allowed";
    String accessToken = login(userKey);
    Long userProfileId = userProfileId(userKey);
    jdbcTemplate.update(
        "insert into admin_account (user_profile_id, created_at) values (?, current_timestamp)",
        userProfileId);

    mockMvc
        .perform(
            get("/api/v1/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound());
  }

  /** 테스트 식별자를 사용하는 가짜 소셜 로그인으로 접근 토큰을 발급한다. */
  private String login(String userKey) throws Exception {
    String nonce = userKey + "-nonce";
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

  /** 테스트 로그인으로 생성된 사용자 프로필 식별자를 조회한다. */
  private Long userProfileId(String userKey) {
    return jdbcTemplate.queryForObject(
        "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
  }
}
