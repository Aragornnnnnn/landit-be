// develop 환경의 관리자 시나리오 목록 API를 검증한다.

package com.landit.landitbe.feature.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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

/** Develop 환경의 관리자 시나리오 목록 API를 검증한다. */
@ActiveProfiles({"develop", "test"})
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminScenarioApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void clearScenarioContent() {
    jdbcTemplate.update("DELETE FROM user_scenario_access");
    jdbcTemplate.update("DELETE FROM user_scenario_progress");
    jdbcTemplate.update("DELETE FROM scenario_question_language_variant");
    jdbcTemplate.update("DELETE FROM scenario_question");
    jdbcTemplate.update("DELETE FROM scenario_language_variant");
    jdbcTemplate.update("DELETE FROM scenario");
    jdbcTemplate.update("DELETE FROM category_language_variant");
    jdbcTemplate.update("DELETE FROM category");
  }

  @DisplayName("관리자는 활성 시나리오만 진행 상태 필드 없이 조회한다.")
  @Test
  void listsOnlyActiveScenariosWithoutUserProgressionFields() throws Exception {
    String accessToken = loginAdmin("admin-scenario-list", "관리자");
    Long adminUserId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM user_profile WHERE role = 'ADMIN' ORDER BY id DESC LIMIT 1",
            Long.class);
    jdbcTemplate.update("UPDATE user_profile SET learning_level = 2 WHERE id = ?", adminUserId);
    seedScenarioContent();

    mockMvc
        .perform(
            get("/api/v1/admin/scenarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.categories.length()").value(1))
        .andExpect(jsonPath("$.data.categories[0].categoryId").value(501))
        .andExpect(jsonPath("$.data.categories[0].scenarios.length()").value(1))
        .andExpect(jsonPath("$.data.categories[0].scenarios[0].scenarioId").value(601))
        .andExpect(jsonPath("$.data.categories[0].scenarios[0].scenarioTitle").value("공개 시나리오"))
        .andExpect(
            jsonPath("$.data.categories[0].scenarios[0].openingPreview.aiOpeningMessage")
                .value("What drink do you want?"))
        .andExpect(
            jsonPath("$.data.categories[0].scenarios[0].openingPreview.character.characterId")
                .value("chloe"))
        .andExpect(
            jsonPath(
                    "$.data.categories[0].scenarios[0].openingPreview.character.ttsVoice"
                        + ".providerVoiceId")
                .value("aura-2-luna-en"))
        .andExpect(jsonPath("$.data.categories[0].scenarios[0].availabilityStatus").doesNotExist())
        .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").doesNotExist())
        .andExpect(jsonPath("$.data.categories[0].scenarios[0].locked").doesNotExist());
  }

  @DisplayName("인증되지 않은 관리자 시나리오 목록 요청은 거부한다.")
  @Test
  void rejectsUnauthenticatedRequest() throws Exception {
    mockMvc.perform(get("/api/v1/admin/scenarios")).andExpect(status().isUnauthorized());
  }

  @DisplayName("일반 사용자의 관리자 시나리오 목록 요청은 거부한다.")
  @Test
  void rejectsNonAdminRequest() throws Exception {
    String accessToken = login("admin-scenario-user-" + UUID.randomUUID(), "일반 사용자");

    mockMvc
        .perform(
            get("/api/v1/admin/scenarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  @DisplayName("관리자 시나리오 목록 API를 OpenAPI 문서에 노출한다.")
  @Test
  void documentsAdminScenarioList() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/scenarios'].get.summary").exists());
  }

  private void seedScenarioContent() {
    insertCategory(501, 1, "ACTIVE", "공개 카테고리");
    insertScenario(601, 501, 601, "AI", "EASY", "ACTIVE");
    insertScenarioVariant(601, "공개 시나리오", "공개 설명", "대화 목표", "먼저 말해보세요.", "ACTIVE");
    insertScenarioQuestion(
        6011, 601, "What beverage would you prefer?", "어떤 음료를 선호하세요?", "LEVEL_4_TO_5");
    insertScenarioQuestion(6012, 601, "What drink do you want?", "어떤 음료를 원해요?", "LEVEL_2_TO_3");

    insertScenario(602, 501, 602, "USER", "EASY", "INACTIVE");
    insertScenarioVariant(602, "비공개 시나리오", "비공개 설명", "대화 목표", "먼저 말해보세요.", "ACTIVE");

    insertScenario(603, 501, 603, "USER", "EASY", "ACTIVE");
    insertScenarioVariant(603, "비공개 언어 콘텐츠", "비공개 설명", "대화 목표", "먼저 말해보세요.", "INACTIVE");

    insertCategory(502, 2, "INACTIVE", "비공개 카테고리");
    insertScenario(604, 502, 604, "USER", "EASY", "ACTIVE");
    insertScenarioVariant(604, "비공개 카테고리 시나리오", "비공개 설명", "대화 목표", "먼저 말해보세요.", "ACTIVE");
  }

  private String loginAdmin(String userKey, String nickname) throws Exception {
    String uniqueUserKey = userKey + "-" + UUID.randomUUID();
    String accessToken = login(uniqueUserKey, nickname);
    Long userProfileId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM user_profile WHERE email = ?",
            Long.class,
            uniqueUserKey + "@example.com");
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

  private void insertScenario(
      long scenarioId,
      long categoryId,
      int displayOrder,
      String firstSpeaker,
      String difficulty,
      String status) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            character_id, display_order, status, created_at, updated_at)
        VALUES (?, ?, 'tutor', ?, ?, 3, 'chloe', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        categoryId,
        difficulty,
        firstSpeaker,
        displayOrder,
        status);
  }

  private void insertScenarioVariant(
      long scenarioId,
      String title,
      String briefing,
      String conversationGoal,
      String userOpeningInstruction,
      String status) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario_language_variant (
            scenario_id, target_locale, base_locale, title, briefing,
            user_opening_instruction, conversation_goal, status, created_at, updated_at)
        VALUES (?, 'EN', 'KR', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        title,
        briefing,
        userOpeningInstruction,
        conversationGoal,
        status);
  }

  private void insertScenarioQuestion(
      long questionId,
      long scenarioId,
      String questionText,
      String questionTranslation,
      String questionLevelGroup) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario_question (
            id, scenario_id, display_order, question_level_group, status, created_at, updated_at)
        VALUES (?, ?, 1, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        questionId,
        scenarioId,
        questionLevelGroup);
    jdbcTemplate.update(
        """
        INSERT INTO scenario_question_language_variant (
            scenario_question_id, target_locale, base_locale, question_text,
            question_translation, audio_url, status, created_at, updated_at)
        VALUES (?, 'EN', 'KR', ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        questionId,
        questionText,
        questionTranslation,
        "https://cdn.example.com/questions/%d.mp3".formatted(questionId));
  }
}
