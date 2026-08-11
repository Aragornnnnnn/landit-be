// develop 환경의 관리자 시나리오 세션 시작 API를 검증한다.

package com.landit.landitbe.feature.session;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/** Develop 환경의 관리자 시나리오 세션 시작 API를 검증한다. */
@ActiveProfiles({"develop", "test"})
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminScenarioSessionApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void clearScenarioSessions() {
    jdbcTemplate.update("DELETE FROM session_history_message_feedback");
    jdbcTemplate.update("DELETE FROM session_history_summary_feedback");
    jdbcTemplate.update("DELETE FROM session_history_artifact");
    jdbcTemplate.update("DELETE FROM session_history_message");
    jdbcTemplate.update("DELETE FROM scenario_session");
    jdbcTemplate.update("DELETE FROM session_history");
    jdbcTemplate.update("DELETE FROM learning_session");
    jdbcTemplate.update("DELETE FROM user_scenario_access");
    jdbcTemplate.update("DELETE FROM user_scenario_progress");
    jdbcTemplate.update("DELETE FROM scenario_language_variant");
    jdbcTemplate.update("DELETE FROM scenario");
    jdbcTemplate.update("DELETE FROM category_language_variant");
    jdbcTemplate.update("DELETE FROM category");
  }

  @AfterEach
  void clearScenarioSessionsAfterTest() {
    clearScenarioSessions();
  }

  @DisplayName("관리자는 이전 시나리오를 완료하지 않아도 다음 시나리오를 시작한다.")
  @Test
  void startsScenarioBeforeCompletingPreviousScenario() throws Exception {
    String accessToken = loginAdmin("admin-scenario-out-of-order", "관리자");
    seedScenarioContent();

    mockMvc
        .perform(
            post("/api/v1/admin/scenarios/{scenarioId}/sessions", 702)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.scenarioId").value(702))
        .andExpect(jsonPath("$.data.sessionType").value("SCENARIO"));
  }

  @DisplayName("관리자는 오늘 다른 시나리오를 완료했어도 추가 시나리오를 시작한다.")
  @Test
  void startsAdditionalScenarioAfterCompletingAnotherToday() throws Exception {
    String userKey = "admin-scenario-start-" + UUID.randomUUID();
    final String accessToken = login(userKey, "관리자");
    long userProfileId = userProfileId(userKey);
    jdbcTemplate.update("UPDATE user_profile SET role = 'ADMIN' WHERE id = ?", userProfileId);
    seedScenarioContent();
    grantScenarioAccess(userProfileId, 701);
    completeScenarioToday(userProfileId, 701);

    mockMvc
        .perform(
            post("/api/v1/scenarios/{scenarioId}/sessions", 702)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("SCENARIO_LOCKED"))
        .andExpect(jsonPath("$.error.message").value("DAILY_SCENARIO_NOT_AVAILABLE"));

    mockMvc
        .perform(
            post("/api/v1/admin/scenarios/{scenarioId}/sessions", 702)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.scenarioId").value(702))
        .andExpect(jsonPath("$.data.sessionType").value("SCENARIO"));
  }

  @DisplayName("관리자도 비활성 시나리오는 시작할 수 없다.")
  @Test
  void rejectsInactiveScenario() throws Exception {
    String accessToken = loginAdmin("admin-scenario-inactive", "관리자");
    seedScenarioContent();

    mockMvc
        .perform(
            post("/api/v1/admin/scenarios/{scenarioId}/sessions", 703)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  @DisplayName("일반 사용자의 관리자 시나리오 세션 시작 요청은 거부한다.")
  @Test
  void rejectsNonAdminRequest() throws Exception {
    String userKey = "admin-scenario-session-user-" + UUID.randomUUID();
    String accessToken = login(userKey, "일반 사용자");

    mockMvc
        .perform(
            post("/api/v1/admin/scenarios/{scenarioId}/sessions", 702)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  @DisplayName("관리자 시나리오 세션 시작 API를 OpenAPI 문서에 노출한다.")
  @Test
  void documentsAdminScenarioSessionStart() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/scenarios/{scenarioId}/sessions'].post.summary")
                .exists());
  }

  private void seedScenarioContent() {
    insertCategory(701, 701, "ACTIVE", "관리자 테스트");
    insertScenario(701, 701, 701, "ACTIVE");
    insertScenarioVariant(701, 701, "첫 번째 시나리오");
    insertScenario(702, 701, 702, "ACTIVE");
    insertScenarioVariant(702, 702, "두 번째 시나리오");
    insertScenario(703, 701, 703, "INACTIVE");
    insertScenarioVariant(703, 703, "비공개 시나리오");
  }

  private String loginAdmin(String userKey, String nickname) throws Exception {
    String uniqueUserKey = userKey + "-" + UUID.randomUUID();
    String accessToken = login(uniqueUserKey, nickname);
    Long userProfileId = userProfileId(uniqueUserKey);
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

  private void grantScenarioAccess(long userProfileId, long scenarioId) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_access (
            user_profile_id, scenario_id, target_locale, granted_at, created_at, updated_at)
        VALUES (?, ?, 'EN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userProfileId,
        scenarioId);
  }

  private void completeScenarioToday(long userProfileId, long scenarioId) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_progress (
            user_profile_id, scenario_id, target_locale, status, best_star_rating,
            best_native_score, completed_count, first_cleared_at, last_played_at,
            created_at, updated_at)
        VALUES (
            ?, ?, 'EN', 'CLEARED', 3.0, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userProfileId,
        scenarioId);
  }

  private void insertCategory(long categoryId, int displayOrder, String status, String name) {
    jdbcTemplate.update(
        """
        INSERT INTO category (id, display_order, status, created_at, updated_at)
        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        categoryId,
        displayOrder,
        status);
    jdbcTemplate.update(
        """
        INSERT INTO category_language_variant (
            category_id, base_locale, name, created_at, updated_at)
        VALUES (?, 'KR', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        categoryId,
        name);
  }

  private void insertScenario(long scenarioId, long categoryId, int displayOrder, String status) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at)
        VALUES (?, ?, 'tutor', 'EASY', 'USER', 1, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        categoryId,
        displayOrder,
        status);
  }

  private void insertScenarioVariant(long variantId, long scenarioId, String title) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario_language_variant (
            id, scenario_id, target_locale, base_locale, title, briefing,
            user_opening_instruction, conversation_goal, status, created_at, updated_at)
        VALUES (
            ?, ?, 'EN', 'KR', ?, '테스트 설명', '먼저 말해보세요.', '대화 목표',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        variantId,
        scenarioId,
        title);
  }
}
