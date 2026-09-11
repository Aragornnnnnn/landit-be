// 유료 기능 API가 구독 상태와 도입 이후 대화 완료 여부에 따라 403으로 제한되는지 검증한다.

package com.landit.landitbe.feature.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 유료 기능 API가 구독 상태와 도입 이후 대화 완료 여부에 따라 403으로 제한되는지 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.subscription.revenuecat.webhook-authorization="
          + PremiumAccessIntegrationTests.WEBHOOK_SECRET,
      "landit.subscription.launched-at=" + PremiumAccessIntegrationTests.LAUNCHED_AT
    })
class PremiumAccessIntegrationTests {

  static final String WEBHOOK_SECRET = "test-revenuecat-webhook-secret";
  static final String LAUNCHED_AT = "2026-09-01T00:00:00+09:00";

  private static final long CATEGORY_ID = 7_003L;
  private static final long SCENARIO_ID = 7_103L;
  private static final long MISSING_ID = 987_654_321L;
  private static final LocalDateTime AFTER_LAUNCH = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
  private static final long EVENT_TIMESTAMP_MS = 4_000_000_000_000L;

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
        VALUES (?, 'KR', '유료 잠금 테스트 카테고리', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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

  /** 비프리미엄 사용자는 대화 완료 전이라도 프리톡 시작·표현 학습·발음 평가를 쓸 수 없다. */
  @Test
  void blocksPremiumOnlyFeaturesForNonPremium() throws Exception {
    String accessToken = login("premium-gate-basic");

    for (MockHttpServletRequestBuilder request :
        new MockHttpServletRequestBuilder[] {
          post("/api/v1/free-talk/sessions")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"startMode\":\"AI_FIRST\"}"),
          post("/api/v1/free-talk/sessions/" + MISSING_ID + "/messages")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"),
          get("/api/v1/expressions/" + MISSING_ID + "/learning-start"),
          get("/api/v1/expressions/" + MISSING_ID + "/practice"),
          post("/api/v1/expressions/" + MISSING_ID + "/learning-finish")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"),
          post("/api/v1/expressions/" + MISSING_ID + "/pronunciation/sentence-analysis")
        }) {
      mockMvc
          .perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.code").value("PREMIUM_REQUIRED"));
    }
  }

  /** 경로를 퍼센트 인코딩해도 컨트롤러 매핑과 같은 디코딩 기준으로 게이트에 걸린다. 매트릭스 변수(;)는 Spring Security 방화벽이 400으로 거절한다. */
  @Test
  void blocksEncodedAndMatrixVariantsOfGatedPaths() throws Exception {
    String accessToken = login("premium-gate-encoded");

    for (String rawPath :
        new String[] {
          "/api/v1/expressions/" + MISSING_ID + "/learning%2Dstart",
          "/api/v1/expressions/" + MISSING_ID + "/learning%2dstart",
        }) {
      mockMvc
          .perform(
              get(URI.create(rawPath)).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.code").value("PREMIUM_REQUIRED"));
    }
  }

  /** 비프리미엄 사용자도 대화 완료 전에는 시나리오 세션 시작·메시지 전송과 조회 API가 게이트를 통과한다. */
  @Test
  void allowsScenarioConversationScopeBeforeCompletion() throws Exception {
    String accessToken = login("premium-gate-free");

    expectNotPremiumRequired(post("/api/v1/scenarios/" + MISSING_ID + "/sessions"), accessToken);
    expectNotPremiumRequired(
        post("/api/v1/sessions/" + MISSING_ID + "/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"),
        accessToken);
    expectNotPremiumRequired(get("/api/v1/free-talk/topics"), accessToken);
    expectNotPremiumRequired(get("/api/v1/free-talk/sessions"), accessToken);
    expectNotPremiumRequired(get("/api/v1/expressions/" + SCENARIO_ID), accessToken);
  }

  /** 도입 이후 대화를 완료한 비프리미엄 사용자는 새 세션 시작과 메시지 전송이 막힌다. */
  @Test
  void blocksNewConversationAfterCompletionForNonPremium() throws Exception {
    String userKey = "premium-gate-completed";
    String accessToken = login(userKey);
    insertClearedProgress(userIdOf(userKey));

    mockMvc
        .perform(
            post("/api/v1/scenarios/" + SCENARIO_ID + "/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("PREMIUM_REQUIRED"));
    mockMvc
        .perform(
            post("/api/v1/sessions/" + MISSING_ID + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("PREMIUM_REQUIRED"));
  }

  /** 대화를 완료한 비프리미엄 사용자도 완료한 세션의 결과 보기와 마이페이지·스트릭·메일함은 쓸 수 있다. */
  @Test
  void allowsResultsAndMyPageAfterCompletionForNonPremium() throws Exception {
    String userKey = "premium-gate-results";
    String accessToken = login(userKey);
    insertClearedProgress(userIdOf(userKey));

    expectNotPremiumRequired(post("/api/v1/sessions/" + MISSING_ID + "/feedback"), accessToken);
    expectNotPremiumRequired(
        get("/api/v1/sessions/" + MISSING_ID + "/messages/1/inner-thought"), accessToken);
    expectNotPremiumRequired(patch("/api/v1/sessions/" + MISSING_ID + "/end"), accessToken);
    expectNotPremiumRequired(get("/api/v1/expressions/" + SCENARIO_ID), accessToken);
    expectNotPremiumRequired(get("/api/v1/me/streak"), accessToken);
    expectNotPremiumRequired(get("/api/v1/mailbox/unread-count"), accessToken);
    expectNotPremiumRequired(get("/api/v1/me/subscription"), accessToken);
  }

  /** 프리미엄 사용자는 대화를 완료했어도 모든 유료 기능 게이트를 통과한다. */
  @Test
  void allowsEverythingForPremiumUser() throws Exception {
    String userKey = "premium-gate-paid";
    String accessToken = login(userKey);
    Long userId = userIdOf(userKey);
    insertClearedProgress(userId);
    activatePremium(userId);

    expectNotPremiumRequired(post("/api/v1/scenarios/" + MISSING_ID + "/sessions"), accessToken);
    expectNotPremiumRequired(
        post("/api/v1/free-talk/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"startMode\":\"AI_FIRST\"}"),
        accessToken);
    expectNotPremiumRequired(
        get("/api/v1/expressions/" + MISSING_ID + "/learning-start"), accessToken);
  }

  /** 인증되지 않은 요청은 게이트 경로에서도 403이 아니라 401을 받는다. */
  @Test
  void rejectsUnauthenticatedWithUnauthorizedNotForbidden() throws Exception {
    mockMvc
        .perform(post("/api/v1/free-talk/sessions").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/v1/scenarios/" + MISSING_ID + "/sessions"))
        .andExpect(status().isUnauthorized());
  }

  /** OpenAPI 문서에 게이트 대상 API의 403 응답이 기술된다. */
  @Test
  void openApiDocsDescribePremiumRequired() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/free-talk/sessions'].post.responses['403']").exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/expressions/{expressionId}/learning-start']"
                        + ".get.responses['403']")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/expressions/{expressionId}/pronunciation/sentence-analysis']"
                        + ".post.responses['403']")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sessions'].post.responses['403']")
                .exists());
  }

  private void expectNotPremiumRequired(MockHttpServletRequestBuilder request, String accessToken)
      throws Exception {
    ResultMatcher notForbidden =
        result ->
            assertThat(result.getResponse().getStatus())
                .as("%s %s", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                .isNotEqualTo(HttpStatus.FORBIDDEN.value());
    mockMvc
        .perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(notForbidden);
  }

  private void insertClearedProgress(Long userId) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_progress (
            user_profile_id, scenario_id, target_locale, status, completed_count,
            first_cleared_at, last_cleared_at, last_played_at, created_at, updated_at
        )
        VALUES (?, ?, 'EN', 'CLEARED', 1, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP)
        """,
        userId,
        SCENARIO_ID,
        AFTER_LAUNCH,
        AFTER_LAUNCH);
  }

  private void activatePremium(Long userId) throws Exception {
    String body =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "INITIAL_PURCHASE",
            "app_user_id": "%d",
            "period_type": "NORMAL",
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """
            .formatted(
                UUID.randomUUID(),
                userId,
                EVENT_TIMESTAMP_MS,
                EVENT_TIMESTAMP_MS + 30L * 24 * 60 * 60 * 1000);
    mockMvc
        .perform(
            post("/webhooks/revenuecat")
                .header(HttpHeaders.AUTHORIZATION, WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
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
