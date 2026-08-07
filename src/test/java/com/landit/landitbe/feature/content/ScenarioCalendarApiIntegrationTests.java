// 시나리오 캘린더 API의 창 구성, 완료일 썸네일, 오늘 배정, 파라미터 검증을 검증한다.

package com.landit.landitbe.feature.content;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
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

/** 시나리오 캘린더 API의 창 구성, 완료일 썸네일, 오늘 배정, 파라미터 검증을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Import(ScenarioCalendarApiIntegrationTests.FixedClockConfiguration.class)
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class ScenarioCalendarApiIntegrationTests {

  private static final String CALENDAR_URL = "/api/v1/scenarios/calendar";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private MutableClock mutableClock;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    // 서울 기준 오늘이 2026-07-30(목)이 되는 시각으로 고정한다.
    mutableClock.setInstant(Instant.parse("2026-07-30T05:00:00Z"));
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
  void calendarRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get(CALENDAR_URL).param("type", "WEEK"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  @Test
  void weekCalendarFillsAllSevenCellsWithTodayAsDefaultDate() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();
    insertScenarioAccess(userId, 301, "2026-07-26 10:00:00");
    insertScenarioAccess(userId, 302, "2026-07-27 21:00:00");

    // 오늘(7/30 목)이 속한 주는 7/26(일)~8/1(토)이다. 301·302 완료 후 배정 시나리오는 303이다.
    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "WEEK")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.type").value("WEEK"))
        .andExpect(jsonPath("$.data.date").value("2026-07-30"))
        .andExpect(jsonPath("$.data.label").value("26년 7월 5주차"))
        .andExpect(jsonPath("$.data.today").value("2026-07-30"))
        .andExpect(jsonPath("$.data.startedAt").value("2026-07-26"))
        .andExpect(jsonPath("$.data.days", hasSize(7)))
        .andExpect(jsonPath("$.data.days[0].date").value("2026-07-26"))
        .andExpect(jsonPath("$.data.days[0].dayOfWeek").value("일"))
        .andExpect(jsonPath("$.data.days[0].completed").value(true))
        .andExpect(jsonPath("$.data.days[0].scenarioId").value(301))
        .andExpect(
            jsonPath("$.data.days[0].thumbnailUrl").value("https://cdn.landit.com/first.png"))
        .andExpect(jsonPath("$.data.days[1].date").value("2026-07-27"))
        .andExpect(jsonPath("$.data.days[1].completed").value(true))
        .andExpect(jsonPath("$.data.days[1].scenarioId").value(302))
        .andExpect(
            jsonPath("$.data.days[1].thumbnailUrl").value("https://cdn.landit.com/second.png"))
        .andExpect(jsonPath("$.data.days[2].date").value("2026-07-28"))
        .andExpect(jsonPath("$.data.days[2].completed").value(false))
        .andExpect(jsonPath("$.data.days[2].scenarioId").value(nullValue()))
        .andExpect(jsonPath("$.data.days[2].thumbnailUrl").value(nullValue()))
        .andExpect(jsonPath("$.data.days[4].date").value("2026-07-30"))
        .andExpect(jsonPath("$.data.days[4].dayOfWeek").value("목"))
        .andExpect(jsonPath("$.data.days[4].completed").value(false))
        .andExpect(jsonPath("$.data.days[4].scenarioId").value(303))
        .andExpect(jsonPath("$.data.days[4].thumbnailUrl").value(nullValue()))
        .andExpect(jsonPath("$.data.days[6].date").value("2026-08-01"))
        .andExpect(jsonPath("$.data.days[6].dayOfWeek").value("토"))
        .andExpect(jsonPath("$.data.days[6].completed").value(false))
        .andExpect(jsonPath("$.data.days[6].scenarioId").value(nullValue()));
  }

  @Test
  void weekCalendarLabelFollowsRequestedDateMonth() throws Exception {
    JsonNode loginResponseBody = login();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();

    // 같은 7/26~8/1 창이라도 기준 날짜가 8/1이면 8월 1주차로 표기한다.
    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "WEEK")
                .param("date", "2026-08-01")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.date").value("2026-08-01"))
        .andExpect(jsonPath("$.data.label").value("26년 8월 1주차"))
        .andExpect(jsonPath("$.data.days[0].date").value("2026-07-26"))
        .andExpect(jsonPath("$.data.days[6].date").value("2026-08-01"));
  }

  @Test
  void monthCalendarReturnsOnlyDaysOfRequestedMonth() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();
    insertScenarioAccess(userId, 301, "2026-06-28 10:00:00");
    insertScenarioAccess(userId, 302, "2026-07-15 10:00:00");

    // MONTH 창은 그 달 1일~말일이라 7월은 31칸이고, 6/28 완료 이력은 창 밖이라 담기지 않는다.
    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "MONTH")
                .param("date", "2026-07-01")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.type").value("MONTH"))
        .andExpect(jsonPath("$.data.label").value("26년 7월"))
        .andExpect(jsonPath("$.data.startedAt").value("2026-06-28"))
        .andExpect(jsonPath("$.data.days", hasSize(31)))
        .andExpect(jsonPath("$.data.days[0].date").value("2026-07-01"))
        .andExpect(jsonPath("$.data.days[0].dayOfWeek").value("수"))
        .andExpect(jsonPath("$.data.days[0].completed").value(false))
        .andExpect(jsonPath("$.data.days[0].scenarioId").value(nullValue()))
        .andExpect(jsonPath("$.data.days[14].date").value("2026-07-15"))
        .andExpect(jsonPath("$.data.days[14].completed").value(true))
        .andExpect(jsonPath("$.data.days[14].scenarioId").value(302))
        .andExpect(
            jsonPath("$.data.days[14].thumbnailUrl").value("https://cdn.landit.com/second.png"))
        .andExpect(jsonPath("$.data.days[29].date").value("2026-07-30"))
        .andExpect(jsonPath("$.data.days[29].completed").value(false))
        .andExpect(jsonPath("$.data.days[29].scenarioId").value(303))
        .andExpect(jsonPath("$.data.days[29].thumbnailUrl").value(nullValue()))
        .andExpect(jsonPath("$.data.days[30].date").value("2026-07-31"))
        .andExpect(jsonPath("$.data.days[30].completed").value(false));
  }

  @Test
  void monthCalendarForFutureMonthReturnsEmptyCells() throws Exception {
    JsonNode loginResponseBody = login();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();

    // 9월 창(9/1~9/30)에는 오늘(7/30)이 없어 배정 칸 없이 빈 칸만 반환한다.
    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "MONTH")
                .param("date", "2026-09-15")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.label").value("26년 9월"))
        .andExpect(jsonPath("$.data.days", hasSize(30)))
        .andExpect(jsonPath("$.data.days[0].date").value("2026-09-01"))
        .andExpect(jsonPath("$.data.days[0].completed").value(false))
        .andExpect(jsonPath("$.data.days[0].scenarioId").value(nullValue()))
        .andExpect(jsonPath("$.data.days[29].date").value("2026-09-30"))
        .andExpect(jsonPath("$.data.days[29].completed").value(false));
  }

  @Test
  void calendarForNewUserFillsOnlyTodayCellWithAssignedScenario() throws Exception {
    JsonNode loginResponseBody = login();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();

    // 완료 이력이 없으면 startedAt은 null이고 오늘 칸에만 첫 배정 시나리오가 담긴다.
    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "WEEK")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.startedAt").value(nullValue()))
        .andExpect(jsonPath("$.data.days[0].scenarioId").value(nullValue()))
        .andExpect(jsonPath("$.data.days[4].date").value("2026-07-30"))
        .andExpect(jsonPath("$.data.days[4].completed").value(false))
        .andExpect(jsonPath("$.data.days[4].scenarioId").value(301))
        .andExpect(jsonPath("$.data.days[4].thumbnailUrl").value(nullValue()))
        .andExpect(jsonPath("$.data.days[6].scenarioId").value(nullValue()));
  }

  @Test
  void calendarMarksTodayCompletedAfterTodayCompletion() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();
    insertScenarioAccess(userId, 301, "2026-07-30 08:00:00");

    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "WEEK")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.startedAt").value("2026-07-30"))
        .andExpect(jsonPath("$.data.days[4].date").value("2026-07-30"))
        .andExpect(jsonPath("$.data.days[4].completed").value(true))
        .andExpect(jsonPath("$.data.days[4].scenarioId").value(301))
        .andExpect(
            jsonPath("$.data.days[4].thumbnailUrl").value("https://cdn.landit.com/first.png"));
  }

  @Test
  void calendarLeavesTodayCellEmptyWhenAllScenariosCleared() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();
    insertScenarioAccess(userId, 301, "2026-07-26 10:00:00");
    insertScenarioAccess(userId, 302, "2026-07-27 10:00:00");
    insertScenarioAccess(userId, 303, "2026-07-28 10:00:00");

    // 모든 시나리오를 완료했으면 오늘 칸에 배정할 시나리오가 없다.
    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "WEEK")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.days[4].date").value("2026-07-30"))
        .andExpect(jsonPath("$.data.days[4].completed").value(false))
        .andExpect(jsonPath("$.data.days[4].scenarioId").value(nullValue()));
  }

  @Test
  void calendarExcludesOtherUserAndOtherLocaleCompletions() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    long otherUserId = login("other@example.com").get("data").get("user").get("userId").asLong();
    seedScenarios();
    insertScenarioAccessWithLocale(userId, 301, "2026-07-27 10:00:00", "KR");
    insertScenarioAccess(otherUserId, 302, "2026-07-28 10:00:00");

    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "WEEK")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.startedAt").value(nullValue()))
        .andExpect(jsonPath("$.data.days[1].date").value("2026-07-27"))
        .andExpect(jsonPath("$.data.days[1].completed").value(false))
        .andExpect(jsonPath("$.data.days[2].date").value("2026-07-28"))
        .andExpect(jsonPath("$.data.days[2].completed").value(false));
  }

  @Test
  void calendarUsesEarliestGrantWhenMultipleGrantsExistOnSameDay() throws Exception {
    JsonNode loginResponseBody = login();
    long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
    final String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedScenarios();
    insertScenarioAccess(userId, 302, "2026-07-27 09:00:00");
    insertScenarioAccess(userId, 301, "2026-07-27 20:00:00");

    mockMvc
        .perform(
            get(CALENDAR_URL)
                .param("type", "WEEK")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.days[1].date").value("2026-07-27"))
        .andExpect(jsonPath("$.data.days[1].scenarioId").value(302))
        .andExpect(
            jsonPath("$.data.days[1].thumbnailUrl").value("https://cdn.landit.com/second.png"));
  }

  @Test
  void calendarRejectsInvalidParameters() throws Exception {
    final String accessToken = login().get("data").get("accessToken").asText();

    // type 값 오류
    expectValidationFailed(
        get(CALENDAR_URL)
            .param("type", "YEAR")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    // type 누락
    expectValidationFailed(
        get(CALENDAR_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    // date 형식 오류
    expectValidationFailed(
        get(CALENDAR_URL)
            .param("type", "WEEK")
            .param("date", "2026-7-1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    // 불가능한 월
    expectValidationFailed(
        get(CALENDAR_URL)
            .param("type", "WEEK")
            .param("date", "2026-13-01")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
  }

  @Test
  void openApiDocumentsCalendarContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/v1/scenarios/calendar'].get.summary").value("시나리오 캘린더 조회"))
        .andExpect(
            jsonPath("$.paths['/api/v1/scenarios/calendar'].get.security[0].bearerAuth").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/scenarios/calendar'].get.responses['200'].description")
                .value("조회 성공"))
        .andExpect(
            jsonPath("$.paths['/api/v1/scenarios/calendar'].get.responses['400'].description")
                .isNotEmpty())
        .andExpect(
            jsonPath("$.paths['/api/v1/scenarios/calendar'].get.responses['401'].description")
                .value("인증 실패"));
  }

  private void expectValidationFailed(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
      throws Exception {
    mockMvc
        .perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  private JsonNode login() throws Exception {
    return login("calendar@example.com");
  }

  private JsonNode login(String email) throws Exception {
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
                                  "idToken":"%s|%s|Calendar User|%s",
                                  "nonce":"%s"
                                }
                        """
                            .formatted(UUID.randomUUID(), email, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
  }

  private void seedScenarios() {
    insertCategory(130, 1, "ACTIVE", "캘린더 카테고리");
    insertScenario(301, 130, 1, "ACTIVE", "https://cdn.landit.com/first.png");
    insertScenarioVariant(301, "첫 번째 시나리오", "ACTIVE");
    insertScenario(302, 130, 2, "ACTIVE", "https://cdn.landit.com/second.png");
    insertScenarioVariant(302, "두 번째 시나리오", "ACTIVE");
    insertScenario(303, 130, 3, "ACTIVE", "https://cdn.landit.com/third.png");
    insertScenarioVariant(303, "세 번째 시나리오", "ACTIVE");
  }

  private void insertScenarioAccess(long userId, long scenarioId, String grantedAt) {
    insertScenarioAccessWithLocale(userId, scenarioId, grantedAt, "EN");
  }

  private void insertScenarioAccessWithLocale(
      long userId, long scenarioId, String grantedAt, String targetLocale) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_access (
            user_profile_id, scenario_id, target_locale, granted_at, created_at, updated_at
        )
        VALUES (?, ?, ?, TIMESTAMP '%s', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """
            .formatted(grantedAt),
        userId,
        scenarioId,
        targetLocale);
  }

  private void insertCategory(
      long categoryId, int displayOrder, String categoryStatus, String categoryName) {
    jdbcTemplate.update(
        """
                        INSERT INTO category (id, display_order, status, created_at, updated_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        categoryId,
        displayOrder,
        categoryStatus);
    jdbcTemplate.update(
        """
                        INSERT INTO category_language_variant (
                            category_id,
                            base_locale,
                            name,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'KR', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        categoryId,
        categoryName);
  }

  private void insertScenario(
      long scenarioId,
      long categoryId,
      int displayOrder,
      String scenarioStatus,
      String thumbnailUrl) {
    jdbcTemplate.update(
        """
                        INSERT INTO scenario (
                            id,
                            category_id,
                            ai_role,
                            difficulty,
                            first_speaker,
                            total_question_count,
                            thumbnail_url,
                            display_order,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'tutor', 'EASY', 'USER', 3, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        categoryId,
        thumbnailUrl,
        displayOrder,
        scenarioStatus);
  }

  private void insertScenarioVariant(long scenarioId, String title, String variantStatus) {
    jdbcTemplate.update(
        """
                        INSERT INTO scenario_language_variant (
                            scenario_id,
                            target_locale,
                            base_locale,
                            title,
                            briefing,
                            user_opening_instruction,
                            conversation_goal,
                            tts_voice_id,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'EN', 'KR', ?, '설명', '먼저 말해보세요.', '목표', NULL, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        title,
        variantStatus);
  }

  @TestConfiguration
  static class FixedClockConfiguration {

    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock(Instant.parse("2026-07-30T05:00:00Z"), ZoneOffset.UTC);
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
