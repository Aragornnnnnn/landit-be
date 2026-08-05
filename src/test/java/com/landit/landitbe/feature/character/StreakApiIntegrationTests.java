// 스트릭 조회 API의 인증, 응답 값과 월 파라미터 검증을 확인한다.

package com.landit.landitbe.feature.character;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.character.service.StreakService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 스트릭 조회 API의 인증, 응답 값과 월 파라미터 검증을 확인한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class StreakApiIntegrationTests {

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final Instant TEST_INSTANT = Instant.parse("2026-07-31T15:00:00Z");
  private static final LocalDate TEST_TODAY = TEST_INSTANT.atZone(KOREA_ZONE_ID).toLocalDate();

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private StreakService streakService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM user_daily_activity");
    jdbcTemplate.update("DELETE FROM user_learning_activity_summary");
  }

  /** 인증되지 않은 요청은 현재 스트릭을 조회할 수 없다. */
  @Test
  void currentStreakRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/me/streak"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  /** 기록이 없는 사용자는 현재 스트릭과 오늘 완료 여부의 기본값을 받는다. */
  @Test
  void currentStreakReturnsDefaultsWithoutActivity() throws Exception {
    LoginResult login = login();

    mockMvc
        .perform(
            get("/api/v1/me/streak")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currentStreakDays").value(0))
        .andExpect(jsonPath("$.data.activeToday").value(false))
        .andExpect(jsonPath("$.data.today").value(TEST_TODAY.toString()))
        .andExpect(jsonPath("$.error").value(nullValue()));
  }

  /** 달력은 요청한 월의 완료 날짜와 전체 스트릭 정보를 반환한다. */
  @Test
  void calendarReturnsRequestedMonthActiveDates() throws Exception {
    LoginResult login = login();
    streakService.recordCompletedConversation(
        login.userId(), LocalDate.of(2026, 7, 12).atTime(12, 0));
    streakService.recordCompletedConversation(
        login.userId(), LocalDate.of(2026, 7, 18).atTime(12, 0));

    mockMvc
        .perform(
            get("/api/v1/me/streak/calendar?year=2026&month=7")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.year").value(2026))
        .andExpect(jsonPath("$.data.month").value(7))
        .andExpect(jsonPath("$.data.today").value(TEST_TODAY.toString()))
        .andExpect(jsonPath("$.data.firstActiveDate").value("2026-07-12"))
        .andExpect(jsonPath("$.data.longestStreakDays").value(1))
        .andExpect(jsonPath("$.data.totalActiveDays").value(2))
        .andExpect(jsonPath("$.data.activeDates[0]").value("2026-07-12"))
        .andExpect(jsonPath("$.data.activeDates[1]").value("2026-07-18"));
  }

  /** 비활성 일별 활동은 최초 활성일로 사용하지 않는다. */
  @Test
  void calendarIgnoresInactiveDayForFirstActiveDate() throws Exception {
    LoginResult login = login();
    jdbcTemplate.update(
        """
        INSERT INTO user_daily_activity (
            user_profile_id,
            activity_date,
            completed_session_count,
            completed_review_count,
            study_seconds,
            review_all_correct_reward_xp,
            active_day,
            created_at,
            updated_at)
        VALUES (?, '2026-07-01', 0, 0, 0, 0, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        login.userId());
    streakService.recordCompletedConversation(
        login.userId(), LocalDate.of(2026, 7, 12).atTime(12, 0));

    mockMvc
        .perform(
            get("/api/v1/me/streak/calendar?year=2026&month=7")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.firstActiveDate").value("2026-07-12"));
  }

  /** 월 범위를 벗어난 요청은 공통 검증 오류를 반환한다. */
  @Test
  void calendarRejectsOutOfRangeMonth() throws Exception {
    LoginResult login = login();

    mockMvc
        .perform(
            get("/api/v1/me/streak/calendar?year=2026&month=13")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  /** 연·월을 생략하면 서버의 KST 오늘이 속한 월을 조회한다. */
  @Test
  void calendarDefaultsToCurrentKstMonth() throws Exception {
    LoginResult login = login();

    mockMvc
        .perform(
            get("/api/v1/me/streak/calendar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.year").value(TEST_TODAY.getYear()))
        .andExpect(jsonPath("$.data.month").value(TEST_TODAY.getMonthValue()))
        .andExpect(jsonPath("$.data.today").value(TEST_TODAY.toString()));
  }

  /** 연도와 월 중 하나만 전달한 달력 요청은 공통 검증 오류를 반환한다. */
  @Test
  void calendarRejectsPartialMonthParameters() throws Exception {
    LoginResult login = login();

    for (String query : new String[] {"year=2026", "month=8"}) {
      mockMvc
          .perform(
              get("/api/v1/me/streak/calendar?" + query)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  /** OpenAPI 문서가 두 스트릭 조회 경로를 노출한다. */
  @Test
  void openApiDocumentsStreakPaths() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/me/streak'].get.summary").value("현재 스트릭 조회"))
        .andExpect(
            jsonPath("$.paths['/api/v1/me/streak/calendar'].get.summary").value("월별 스트릭 달력 조회"));
  }

  private LoginResult login() throws Exception {
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
                          "idToken":"%s|streak@example.com|Streak User|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(UUID.randomUUID(), nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
    return new LoginResult(
        body.get("data").get("user").get("userId").asLong(),
        body.get("data").get("accessToken").asText());
  }

  private record LoginResult(long userId, String accessToken) {}

  @TestConfiguration
  static class FixedClockConfiguration {

    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
    }
  }
}
