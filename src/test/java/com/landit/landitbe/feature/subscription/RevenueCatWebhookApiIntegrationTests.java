// RevenueCat 웹훅 수신 API의 인증과 이벤트별 구독 상태 갱신을 검증한다.

package com.landit.landitbe.feature.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Map;
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
import org.springframework.test.web.servlet.ResultActions;

/** RevenueCat 웹훅 수신 API의 인증과 이벤트별 구독 상태 갱신을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.subscription.revenuecat.webhook-authorization="
          + RevenueCatWebhookApiIntegrationTests.WEBHOOK_SECRET
    })
class RevenueCatWebhookApiIntegrationTests {

  static final String WEBHOOK_SECRET = "test-revenuecat-webhook-secret";

  private static final long BASE_EVENT_TIMESTAMP_MS = 1_756_000_000_000L;
  private static final long EXPIRATION_MS = BASE_EVENT_TIMESTAMP_MS + 30L * 24 * 60 * 60 * 1000;

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 최초 구매 이벤트는 프리미엄을 켜고 만료 시각을 저장한다. */
  @Test
  void activatesPremiumOnInitialPurchase() throws Exception {
    Long userId = createUser("rc-initial");

    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
    assertThat(subscriptionExpiresAt(userId)).isNotNull();
  }

  /** 갱신 이벤트는 해지 예약 상태였더라도 다시 활성 구독으로 되돌린다. */
  @Test
  void activatesPremiumOnRenewal() throws Exception {
    Long userId = createUser("rc-renewal");
    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());
    postWebhook(WEBHOOK_SECRET, event("CANCELLATION", userId, BASE_EVENT_TIMESTAMP_MS + 1_000))
        .andExpect(status().isOk());

    postWebhook(WEBHOOK_SECRET, event("RENEWAL", userId, BASE_EVENT_TIMESTAMP_MS + 2_000))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
  }

  /** 해지 이벤트는 해지 예약으로 표시하고, 해지 철회 이벤트는 다시 활성으로 되돌린다. */
  @Test
  void schedulesCancellationAndRestoresOnUncancellation() throws Exception {
    Long userId = createUser("rc-cancel");
    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());

    postWebhook(WEBHOOK_SECRET, event("CANCELLATION", userId, BASE_EVENT_TIMESTAMP_MS + 1_000))
        .andExpect(status().isOk());
    assertThat(subscriptionStatus(userId)).isEqualTo("CANCELED");
    assertThat(subscriptionExpiresAt(userId)).isNotNull();

    postWebhook(WEBHOOK_SECRET, event("UNCANCELLATION", userId, BASE_EVENT_TIMESTAMP_MS + 2_000))
        .andExpect(status().isOk());
    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
  }

  /** 환불은 CUSTOMER_SUPPORT 사유의 해지 이벤트로 오며 프리미엄을 즉시 끈다. */
  @Test
  void deactivatesPremiumOnRefund() throws Exception {
    Long userId = createUser("rc-refund");
    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());

    postWebhook(
            WEBHOOK_SECRET,
            event(
                "CANCELLATION",
                userId,
                BASE_EVENT_TIMESTAMP_MS + 1_000,
                Map.of("cancel_reason", "CUSTOMER_SUPPORT")))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("EXPIRED");
    assertThat(subscriptionExpiresAt(userId)).isNull();
  }

  /** 만료 이벤트는 프리미엄을 끄고 만료 시각을 비운다. */
  @Test
  void deactivatesPremiumOnExpiration() throws Exception {
    Long userId = createUser("rc-expire");
    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());

    postWebhook(WEBHOOK_SECRET, event("EXPIRATION", userId, BASE_EVENT_TIMESTAMP_MS + 1_000))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("EXPIRED");
    assertThat(subscriptionExpiresAt(userId)).isNull();
  }

  /** 이미 반영한 이벤트보다 오래된 이벤트가 뒤늦게 도착하면 무시한다. */
  @Test
  void ignoresStaleEvent() throws Exception {
    Long userId = createUser("rc-stale");
    postWebhook(WEBHOOK_SECRET, event("EXPIRATION", userId, BASE_EVENT_TIMESTAMP_MS + 5_000))
        .andExpect(status().isOk());

    postWebhook(WEBHOOK_SECRET, event("RENEWAL", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("EXPIRED");
  }

  /** 익명 App User ID로 온 이벤트도 aliases에 Landit 사용자 ID가 있으면 반영한다. */
  @Test
  void resolvesUserFromAliasesWhenAppUserIdIsAnonymous() throws Exception {
    Long userId = createUser("rc-alias");
    String body =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "INITIAL_PURCHASE",
            "app_user_id": "$RCAnonymousID:abc",
            "original_app_user_id": "$RCAnonymousID:abc",
            "aliases": ["$RCAnonymousID:abc", "%d"],
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """
            .formatted(UUID.randomUUID(), userId, BASE_EVENT_TIMESTAMP_MS, EXPIRATION_MS);

    postWebhook(WEBHOOK_SECRET, body).andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
  }

  /** 구독 상태와 무관한 이벤트나 존재하지 않는 사용자의 이벤트는 200으로 응답하고 상태를 바꾸지 않는다. */
  @Test
  void acknowledgesIrrelevantOrUnmatchedEvents() throws Exception {
    Long userId = createUser("rc-ignore");

    postWebhook(WEBHOOK_SECRET, event("TEST", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());
    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", 987_654_321L, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("NONE");
  }

  /** Authorization 헤더가 없거나 설정값과 다르면 401로 거절하고 상태를 바꾸지 않는다. */
  @Test
  void rejectsMissingOrWrongAuthorization() throws Exception {
    Long userId = createUser("rc-unauthorized");
    String body = event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS);

    postWebhook(null, body)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("WEBHOOK_UNAUTHORIZED"));
    postWebhook("wrong-secret", body).andExpect(status().isUnauthorized());
    postWebhook("Bearer " + WEBHOOK_SECRET, body).andExpect(status().isUnauthorized());

    assertThat(subscriptionStatus(userId)).isEqualTo("NONE");
  }

  /** 이벤트 객체나 type이 없는 본문은 400으로 거절한다. */
  @Test
  void rejectsMalformedBody() throws Exception {
    postWebhook(WEBHOOK_SECRET, "{\"api_version\":\"1.0\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    postWebhook(WEBHOOK_SECRET, "{\"event\":{\"app_user_id\":\"1\"}}")
        .andExpect(status().isBadRequest());
  }

  /** OpenAPI 문서에는 웹훅 경로를 공개하지 않는다. */
  @Test
  void openApiDocsHideWebhookPath() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/webhooks/revenuecat']").doesNotExist());
  }

  private ResultActions postWebhook(String authorization, String body) throws Exception {
    var request =
        post("/webhooks/revenuecat").contentType(MediaType.APPLICATION_JSON).content(body);
    if (authorization != null) {
      request.header(HttpHeaders.AUTHORIZATION, authorization);
    }
    return mockMvc.perform(request);
  }

  private static String event(String type, Long userId, long eventTimestampMs) {
    return event(type, userId, eventTimestampMs, Map.of());
  }

  private static String event(
      String type, Long userId, long eventTimestampMs, Map<String, String> extraFields) {
    StringBuilder extra = new StringBuilder();
    extraFields.forEach((key, value) -> extra.append(",\"%s\":\"%s\"".formatted(key, value)));
    String template =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "%s",
            "app_user_id": "%d",
            "original_app_user_id": "%d",
            "aliases": ["%d"],
            "product_id": "landit_premium_monthly",
            "environment": "SANDBOX",
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d,
            "unknown_field": {"nested": true}%s
          }
        }
        """;
    return template.formatted(
        UUID.randomUUID(), type, userId, userId, userId, eventTimestampMs, EXPIRATION_MS, extra);
  }

  private String subscriptionStatus(Long userId) {
    return jdbcTemplate.queryForObject(
        "select subscription_status from user_profile where id = ?", String.class, userId);
  }

  private Timestamp subscriptionExpiresAt(Long userId) {
    return jdbcTemplate.queryForObject(
        "select subscription_expires_at from user_profile where id = ?", Timestamp.class, userId);
  }

  /** 가짜 소셜 로그인으로 사용자를 만들고 user_profile ID를 반환한다. */
  private Long createUser(String userKey) throws Exception {
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
    assertThat(body.get("success").asBoolean()).isTrue();
    return jdbcTemplate.queryForObject(
        "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
  }
}
