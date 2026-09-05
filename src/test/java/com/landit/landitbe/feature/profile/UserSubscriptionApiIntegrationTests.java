// 사용자 구독 상태 조회 API의 인증·계약과 웹훅 반영 결과를 검증한다.

package com.landit.landitbe.feature.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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

/** 사용자 구독 상태 조회 API의 인증·계약과 웹훅 반영 결과를 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.subscription.revenuecat.webhook-authorization="
          + UserSubscriptionApiIntegrationTests.WEBHOOK_SECRET
    })
class UserSubscriptionApiIntegrationTests {

  static final String WEBHOOK_SECRET = "test-revenuecat-webhook-secret";

  private static final long EVENT_TIMESTAMP_MS = 1_756_000_000_000L;
  private static final long EXPIRATION_MS = EVENT_TIMESTAMP_MS + 30L * 24 * 60 * 60 * 1000;

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

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
        .andExpect(jsonPath("$.data.expiresAt").isEmpty());
  }

  /** 웹훅으로 무료 체험 구매가 반영되면 ACTIVE 상태, TRIAL 기간 종류, 만료 시각이 조회된다. */
  @Test
  void returnsActiveSubscriptionAfterWebhookPurchase() throws Exception {
    String userKey = "subscription-active";
    String accessToken = login(userKey);
    Long userId =
        jdbcTemplate.queryForObject(
            "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
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

  /** 인증되지 않은 사용자는 구독 상태를 조회할 수 없다. */
  @Test
  void rejectsUnauthenticatedSubscriptionRequest() throws Exception {
    mockMvc.perform(get("/api/v1/me/subscription")).andExpect(status().isUnauthorized());
  }

  /** OpenAPI 문서에 구독 상태 조회 API를 공개한다. */
  @Test
  void openApiDocsDescribeSubscriptionApi() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.responses['401']").exists());
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
