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
import com.landit.landitbe.feature.session.domain.SessionType;
import java.time.LocalDate;
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
        .andExpect(jsonPath("$.error").value(nullValue()));
  }

  /** 달력은 요청한 월의 완료 날짜와 전체 스트릭 정보를 반환한다. */
  @Test
  void calendarReturnsRequestedMonthActiveDates() throws Exception {
    LoginResult login = login();
    streakService.recordCompletedConversation(
        login.userId(), SessionType.SCENARIO, LocalDate.of(2026, 7, 12).atTime(12, 0));
    streakService.recordCompletedConversation(
        login.userId(), SessionType.FREE_TALK, LocalDate.of(2026, 7, 18).atTime(12, 0));

    mockMvc
        .perform(
            get("/api/v1/me/streak/calendar?year=2026&month=7")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.year").value(2026))
        .andExpect(jsonPath("$.data.month").value(7))
        .andExpect(jsonPath("$.data.streakStartedDate").value("2026-07-12"))
        .andExpect(jsonPath("$.data.longestStreakDays").value(1))
        .andExpect(jsonPath("$.data.totalActiveDays").value(2))
        .andExpect(jsonPath("$.data.activeDates[0]").value("2026-07-12"))
        .andExpect(jsonPath("$.data.activeDates[1]").value("2026-07-18"));
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
}
