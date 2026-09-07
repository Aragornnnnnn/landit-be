// 사용자 구독 상태 조회 API의 인증·계약, 웹훅 반영 결과, 도입 이후 대화 완료 판정을 검증한다.

package com.landit.landitbe.feature.subscription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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

/** 사용자 구독 상태 조회 API의 인증·계약, 웹훅 반영 결과, 도입 이후 대화 완료 판정을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.subscription.revenuecat.webhook-authorization="
          + UserSubscriptionApiIntegrationTests.WEBHOOK_SECRET,
      "landit.subscription.launched-at=" + UserSubscriptionApiIntegrationTests.LAUNCHED_AT
    })
class UserSubscriptionApiIntegrationTests {

  static final String WEBHOOK_SECRET = "test-revenuecat-webhook-secret";
  static final String LAUNCHED_AT = "2026-09-01T00:00:00+09:00";

  private static final long EVENT_TIMESTAMP_MS = 1_756_000_000_000L;
  private static final long EXPIRATION_MS = EVENT_TIMESTAMP_MS + 30L * 24 * 60 * 60 * 1000;
  private static final long CATEGORY_ID = 7_001L;
  private static final long SCENARIO_ID = 7_101L;
  private static final LocalDateTime BEFORE_LAUNCH = LocalDateTime.of(2026, 8, 31, 23, 59, 59);
  private static final LocalDateTime AFTER_LAUNCH = LocalDateTime.of(2026, 9, 1, 0, 0, 0);

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 시나리오 진행도가 참조할 카테고리와 시나리오를 준비한다. */
  @BeforeEach
  void seedScenario() {
    jdbcTemplate.update("DELETE FROM user_scenario_progress WHERE scenario_id = ?", SCENARIO_ID);
    jdbcTemplate.update("DELETE FROM scenario WHERE id = ?", SCENARIO_ID);
    jdbcTemplate.update("DELETE FROM category_language_variant WHERE category_id = ?", CATEGORY_ID);
    jdbcTemplate.update("DELETE FROM category WHERE id = ?", CATEGORY_ID);
    jdbcTemplate.update(
        """
        INSERT INTO category (id, display_order, status, created_at, updated_at)
        VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        CATEGORY_ID,
        CATEGORY_ID);
    jdbcTemplate.update(
        """
        INSERT INTO category_language_variant (category_id, base_locale, name, created_at, updated_at)
        VALUES (?, 'KR', '구독 테스트 카테고리', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        CATEGORY_ID);
    jdbcTemplate.update(
        """
        INSERT INTO scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            character_id, display_order, status, created_at, updated_at
        )
        VALUES (?, ?, 'tutor', 'EASY', 'USER', 3, 'chloe', ?, 'ACTIVE', CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP)
        """,
        SCENARIO_ID,
        CATEGORY_ID,
        SCENARIO_ID);
  }

  /** 구독 이력이 없는 사용자는 NONE 상태에 프리미엄이 꺼진 것으로 조회된다. */
  @Test
  void returnsNoneSubscriptionForNewUser() throws Exception {
    String accessToken = login("subscription-free");

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.subscriptionStatus").value("NONE"))
        .andExpect(jsonPath("$.data.premium").value(false))
        .andExpect(jsonPath("$.data.periodType").isEmpty())
        .andExpect(jsonPath("$.data.expiresAt").isEmpty())
        .andExpect(jsonPath("$.data.conversationCompletedSinceLaunch").value(false));
  }

  /** 웹훅으로 무료 체험 구매가 반영되면 ACTIVE 상태, TRIAL 기간 종류, 만료 시각이 조회된다. */
  @Test
  void returnsActiveSubscriptionAfterWebhookPurchase() throws Exception {
    String userKey = "subscription-active";
    String accessToken = login(userKey);
    Long userId = userIdOf(userKey);
    String body =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "INITIAL_PURCHASE",
            "app_user_id": "%d",
            "period_type": "TRIAL",
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """
            .formatted(UUID.randomUUID(), userId, EVENT_TIMESTAMP_MS, EXPIRATION_MS);
    mockMvc
        .perform(
            post("/webhooks/revenuecat")
                .header(HttpHeaders.AUTHORIZATION, WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.data.premium").value(true))
        .andExpect(jsonPath("$.data.periodType").value("TRIAL"))
        .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
  }

  /** 도입 시점 이후에 시나리오를 끝까지 완료한 사용자는 대화 완료로 조회된다. */
  @Test
  void marksConversationCompletedWhenScenarioClearedAfterLaunch() throws Exception {
    String userKey = "subscription-cleared-after";
    String accessToken = login(userKey);
    insertScenarioProgress(userIdOf(userKey), "CLEARED", AFTER_LAUNCH);

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.conversationCompletedSinceLaunch").value(true));
  }

  /** 도입 시점 전에만 완료한 기존 사용자는 대화 완료로 보지 않는다. */
  @Test
  void ignoresScenarioClearedBeforeLaunch() throws Exception {
    String userKey = "subscription-cleared-before";
    String accessToken = login(userKey);
    insertScenarioProgress(userIdOf(userKey), "CLEARED", BEFORE_LAUNCH);

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.conversationCompletedSinceLaunch").value(false));
  }

  /** 시작만 하고 끝까지 완료하지 않은 시나리오는 대화 완료로 보지 않는다. */
  @Test
  void ignoresScenarioOnlyStarted() throws Exception {
    String userKey = "subscription-in-progress";
    String accessToken = login(userKey);
    insertScenarioProgress(userIdOf(userKey), "IN_PROGRESS", null);

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.conversationCompletedSinceLaunch").value(false));
  }

  /** 인증되지 않은 사용자는 구독 상태를 조회할 수 없다. */
  @Test
  void rejectsUnauthenticatedSubscriptionRequest() throws Exception {
    mockMvc.perform(get("/api/v1/me/subscription")).andExpect(status().isUnauthorized());
  }

  /** OpenAPI 문서에 구독 상태 조회 API와 대화 완료 필드를 Subscription 태그로 공개한다. */
  @Test
  void openApiDocsDescribeSubscriptionApi() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.tags[0]").value("Subscription"))
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.responses['401']").exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.UserSubscriptionResponse.properties"
                        + ".conversationCompletedSinceLaunch")
                .exists());
  }

  private void insertScenarioProgress(Long userId, String status, LocalDateTime lastClearedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_progress (
            user_profile_id, scenario_id, target_locale, status, completed_count,
            first_cleared_at, last_cleared_at, last_played_at, created_at, updated_at
        )
        VALUES (?, ?, 'EN', ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        SCENARIO_ID,
        status,
        lastClearedAt == null ? 0 : 1,
        lastClearedAt,
        lastClearedAt);
  }

  private Long userIdOf(String userKey) {
    return jdbcTemplate.queryForObject(
        "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
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
