// 날짜별 시나리오 조회 API의 완료 이력 응답을 검증한다.

package com.landit.landitbe.feature.content;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/** 날짜별 시나리오 조회 API의 완료 이력 응답을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Import(DailyScenarioApiIntegrationTests.FixedClockConfiguration.class)
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class DailyScenarioApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private MutableClock mutableClock;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mutableClock.setInstant(Instant.parse("2026-07-28T14:00:00Z"));
    clearScenarioData();
  }

  @AfterEach
  void tearDown() {
    clearScenarioData();
  }

  private void clearScenarioData() {
    jdbcTemplate.update("DELETE FROM session_history_message_feedback");
    jdbcTemplate.update("DELETE FROM session_history_summary_feedback");
    jdbcTemplate.update("DELETE FROM session_history_artifact");
    jdbcTemplate.update("DELETE FROM session_history_message");
    jdbcTemplate.update("DELETE FROM scenario_session");
    jdbcTemplate.update("DELETE FROM session_history");
    jdbcTemplate.update("DELETE FROM learning_session");
    jdbcTemplate.update("DELETE FROM user_scenario_access");
    jdbcTemplate.update("DELETE FROM user_writing_expression_completion");
    jdbcTemplate.update("DELETE FROM writing_expression");
    jdbcTemplate.update("DELETE FROM user_scenario_progress");
    jdbcTemplate.update("DELETE FROM scenario_question_language_variant");
    jdbcTemplate.update("DELETE FROM scenario_question");
    jdbcTemplate.update("DELETE FROM scenario_language_variant");
    jdbcTemplate.update("DELETE FROM scenario");
    jdbcTemplate.update("DELETE FROM category_language_variant");
    jdbcTemplate.update("DELETE FROM category");
  }

  @Test
  void dailyScenarioReturnsNoScenarioForPastDateWithoutCompletion() throws Exception {
    JsonNode loginResponseBody = login();
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();

    mockMvc
        .perform(
            get("/api/v1/scenarios/daily")
                .queryParam("date", "2026-07-27")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.date").value("2026-07-27"))
        .andExpect(jsonPath("$.data.playable").value(false))
        .andExpect(jsonPath("$.data.scenario").value(nullValue()));
  }

  @Test
  void dailyScenarioReturnsLowestIdCurrentScenarioAndExpressionProgress() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    seedDailyScenarios();
    long firstExpressionId = insertWritingExpression(100, 1);
    insertWritingExpression(100, 2);
    markExpressionCompleted(userId, 100, firstExpressionId);
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();

    mockMvc
        .perform(
            get("/api/v1/scenarios/daily")
                .queryParam("date", "2026-07-28")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.date").value("2026-07-28"))
        .andExpect(jsonPath("$.data.playable").value(true))
        .andExpect(jsonPath("$.data.scenario.scenarioId").value(100))
        .andExpect(jsonPath("$.data.scenario.dailyScenarioType").value("NEW"))
        .andExpect(jsonPath("$.data.scenario.completed").value(false))
        .andExpect(jsonPath("$.data.scenario.completedAt").value(nullValue()))
        .andExpect(jsonPath("$.data.scenario.expressionCount").value(2))
        .andExpect(jsonPath("$.data.scenario.completedExpressionCount").value(1));
  }

  @Test
  void dailyScenarioReturnsClearedScenarioForPastCompletionDate() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    seedDailyScenarios();
    insertScenarioAccess(userId, 100, "2026-07-27 21:10:00");
    insertScenarioProgress(userId, 100, new BigDecimal("3.0"));
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();

    mockMvc
        .perform(
            get("/api/v1/scenarios/daily")
                .queryParam("date", "2026-07-27")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.playable").value(true))
        .andExpect(jsonPath("$.data.scenario.scenarioId").value(100))
        .andExpect(jsonPath("$.data.scenario.dailyScenarioType").value("CLEARED"))
        .andExpect(jsonPath("$.data.scenario.completed").value(true))
        .andExpect(jsonPath("$.data.scenario.completedAt").value("2026-07-27T21:10:00+09:00"))
        .andExpect(jsonPath("$.data.scenario.starRating").value(3.0));
  }

  @Test
  void dailyScenarioReturnsClearedScenarioWhenCompletedToday() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    seedDailyScenarios();
    insertScenarioAccess(userId, 100, "2026-07-28 21:10:00");
    insertScenarioProgress(userId, 100, new BigDecimal("2.5"));
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();

    mockMvc
        .perform(
            get("/api/v1/scenarios/daily")
                .queryParam("date", "2026-07-28")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.playable").value(true))
        .andExpect(jsonPath("$.data.scenario.scenarioId").value(100))
        .andExpect(jsonPath("$.data.scenario.dailyScenarioType").value("CLEARED"))
        .andExpect(jsonPath("$.data.scenario.completed").value(true));
  }

  @Test
  void dailyScenarioReturnsRetryForPreviousDayUncompletedSession() throws Exception {
    JsonNode loginResponseBody = login();
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedDailyScenarios();

    MvcResult startResult =
        mockMvc
            .perform(
                post("/api/v1/scenarios/100/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isCreated())
            .andReturn();
    long sessionId =
        objectMapper
            .readTree(startResult.getResponse().getContentAsByteArray())
            .get("data")
            .get("sessionId")
            .asLong();
    jdbcTemplate.update(
        "UPDATE learning_session SET started_at = TIMESTAMP '2026-07-27 10:00:00' WHERE id = ?",
        sessionId);

    mockMvc
        .perform(
            get("/api/v1/scenarios/daily")
                .queryParam("date", "2026-07-28")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.playable").value(true))
        .andExpect(jsonPath("$.data.scenario.scenarioId").value(100))
        .andExpect(jsonPath("$.data.scenario.dailyScenarioType").value("RETRY"))
        .andExpect(jsonPath("$.data.scenario.completed").value(false));
  }

  @Test
  void dailyScenarioRejectsFutureDate() throws Exception {
    JsonNode loginResponseBody = login();
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();

    mockMvc
        .perform(
            get("/api/v1/scenarios/daily")
                .queryParam("date", "2026-07-29")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
  }

  @Test
  void scenarioListApiIsRemoved() throws Exception {
    JsonNode loginResponseBody = login();
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();

    mockMvc
        .perform(
            get("/api/v1/scenarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  private JsonNode login() throws Exception {
    String nonce = UUID.randomUUID().toString();
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "provider":"GOOGLE",
                          "idToken":"%s|daily-scenario@example.com|Daily Scenario User|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(UUID.randomUUID(), nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
  }

  private void seedDailyScenarios() {
    insertCategory(10, 1);
    insertScenario(100, 10, 2, "USER", "EASY");
    insertScenarioVariant(100, "첫 번째 시나리오", "첫 번째 시나리오 설명", "첫 번째 목표", "먼저 말해보세요.");
    insertScenario(101, 10, 1, "AI", "NORMAL");
    insertScenarioVariant(101, "두 번째 시나리오", "두 번째 시나리오 설명", "두 번째 목표", null);
  }

  private void insertCategory(long categoryId, int displayOrder) {
    jdbcTemplate.update(
        """
        INSERT INTO category (id, display_order, status, created_at, updated_at)
        VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        categoryId,
        displayOrder);
    jdbcTemplate.update(
        """
        INSERT INTO category_language_variant (category_id, base_locale, name, created_at, updated_at)
        VALUES (?, 'KR', '테스트 카테고리', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        categoryId);
  }

  private void insertScenario(
      long scenarioId, long categoryId, int displayOrder, String firstSpeaker, String difficulty) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at
        )
        VALUES (?, ?, 'tutor', ?, ?, 3, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        categoryId,
        difficulty,
        firstSpeaker,
        displayOrder);
  }

  private void insertScenarioVariant(
      long scenarioId,
      String title,
      String briefing,
      String conversationGoal,
      String userOpeningInstruction) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario_language_variant (
            scenario_id, target_locale, base_locale, title, briefing, user_opening_instruction,
            conversation_goal, status, created_at, updated_at
        )
        VALUES (?, 'EN', 'KR', ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        title,
        briefing,
        userOpeningInstruction,
        conversationGoal);
  }

  private long insertWritingExpression(long scenarioId, int displayOrder) {
    jdbcTemplate.update(
        """
        INSERT INTO writing_expression (
            scenario_id, expression_type, usage_frequency_level, target_locale, base_locale,
            display_order, target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            practice_examples_payload, status, created_at, updated_at
        )
        VALUES (?, 'DAILY_ROUTINE', 'BASIC', 'EN', 'KR', ?, 'expression', '표현',
                'usage summary', 'usage description', 'sample sentence', '샘플 문장',
                ARRAY['sample'], ARRAY['sample', 'choice'], CAST('[]' AS jsonb), 'ACTIVE',
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        displayOrder);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM writing_expression WHERE scenario_id = ? AND display_order = ?",
        Long.class,
        scenarioId,
        displayOrder);
  }

  private void markExpressionCompleted(long userId, long scenarioId, long expressionId) {
    jdbcTemplate.update(
        """
        INSERT INTO user_writing_expression_completion (
            user_profile_id, scenario_id, writing_expression_id, completed_at, last_completed_at
        )
        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        scenarioId,
        expressionId);
  }

  private void insertScenarioAccess(long userId, long scenarioId, String grantedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_access (
            user_profile_id, scenario_id, target_locale, granted_at, created_at, updated_at
        )
        VALUES (?, ?, 'EN', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        scenarioId,
        LocalDateTime.parse(grantedAt.replace(' ', 'T')));
  }

  private void insertScenarioProgress(long userId, long scenarioId, BigDecimal starRating) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_progress (
            user_profile_id, scenario_id, target_locale, status, best_star_rating,
            best_native_score, completed_count, first_cleared_at, last_played_at, created_at, updated_at
        )
        VALUES (?, ?, 'EN', 'CLEARED', ?, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        scenarioId,
        starRating);
  }

  @TestConfiguration
  static class FixedClockConfiguration {

    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock(Instant.parse("2026-07-28T14:00:00Z"), ZoneOffset.UTC);
    }
  }

  static class MutableClock extends java.time.Clock {

    private Instant instant;
    private final ZoneId zoneId;

    MutableClock(Instant instant, ZoneId zoneId) {
      this.instant = instant;
      this.zoneId = zoneId;
    }

    void setInstant(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return zoneId;
    }

    @Override
    public java.time.Clock withZone(ZoneId zone) {
      return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
