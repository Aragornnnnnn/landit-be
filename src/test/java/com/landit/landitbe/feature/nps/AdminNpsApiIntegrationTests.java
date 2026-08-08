// 관리자 NPS 목록 API의 정렬, 페이지네이션, 작성자 정보를 검증한다.

package com.landit.landitbe.feature.nps;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

/** 관리자 NPS 목록 API의 정렬, 페이지네이션, 작성자 정보를 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminNpsApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void clearNpsResponses() {
    jdbcTemplate.update("DELETE FROM nps_response");
  }

  @Test
  void listsNpsResponsesWithAuthorsInLatestOrder() throws Exception {
    final String accessToken = loginAdmin();
    String olderUserKey = "nps-older-" + UUID.randomUUID();
    String latestUserKey = "nps-latest-" + UUID.randomUUID();
    login(olderUserKey, "오래된 사용자");
    login(latestUserKey, "최근 사용자");
    insertNps(olderUserKey, 2, "오래된 의견", "2026-08-08 10:00:00");
    insertNps(latestUserKey, 5, null, "2026-08-08 11:00:00");

    mockMvc
        .perform(
            get("/api/v1/admin/nps-responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].score").value(5))
        .andExpect(jsonPath("$.data.items[0].opinionText").doesNotExist())
        .andExpect(jsonPath("$.data.items[0].userNickname").value("최근 사용자"))
        .andExpect(jsonPath("$.data.items[1].score").value(2))
        .andExpect(jsonPath("$.data.items[1].userEmail").value(olderUserKey + "@example.com"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void appliesPaginationAndRejectsInvalidPageParameters() throws Exception {
    String accessToken = loginAdmin();
    for (int index = 0; index < 3; index++) {
      String userKey = "nps-page-" + index + "-" + UUID.randomUUID();
      login(userKey, "페이지 사용자");
      insertNps(userKey, index + 1, "의견", "2026-08-08 10:0" + index + ":00");
    }

    mockMvc
        .perform(
            get("/api/v1/admin/nps-responses")
                .param("page", "0")
                .param("size", "2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(true));

    mockMvc
        .perform(
            get("/api/v1/admin/nps-responses")
                .param("page", "-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            get("/api/v1/admin/nps-responses")
                .param("size", "51")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUnauthenticatedAndNonAdminRequests() throws Exception {
    mockMvc.perform(get("/api/v1/admin/nps-responses")).andExpect(status().isUnauthorized());
    String accessToken = login("nps-normal-" + UUID.randomUUID(), "일반 사용자");

    mockMvc
        .perform(
            get("/api/v1/admin/nps-responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void documentsAdminNpsList() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/nps-responses'].get.summary").exists());
  }

  private String loginAdmin() throws Exception {
    String userKey = "nps-admin-" + UUID.randomUUID();
    String accessToken = login(userKey, "관리자");
    Long userProfileId = userProfileId(userKey);
    jdbcTemplate.update("UPDATE user_profile SET role = 'ADMIN' WHERE id = ?", userProfileId);
    return accessToken;
  }

  private String login(String userKey, String nickname) throws Exception {
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
                            .formatted(userKey, userKey, nickname, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }

  private long userProfileId(String userKey) {
    return jdbcTemplate.queryForObject(
        "SELECT id FROM user_profile WHERE email = ?", Long.class, userKey + "@example.com");
  }

  private void insertNps(String userKey, int score, String opinion, String submittedAt) {
    jdbcTemplate.update(
        "INSERT INTO nps_response (user_profile_id, score, opinion_text, created_at) VALUES (?, ?, ?, ?)",
        userProfileId(userKey),
        score,
        opinion,
        Timestamp.valueOf(submittedAt));
  }
}
