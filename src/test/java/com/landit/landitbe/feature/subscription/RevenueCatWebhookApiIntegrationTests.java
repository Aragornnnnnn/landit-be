// RevenueCat 웹훅 수신 API의 인증, 이벤트별 구독 상태 갱신, 결제 이력 저장을 검증한다.

package com.landit.landitbe.feature.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
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

/** RevenueCat 웹훅 수신 API의 인증, 이벤트별 구독 상태 갱신, 결제 이력 저장을 검증한다. */
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

  /** 무료 체험 시작은 TRIAL 기간 종류로, 유료 전환 갱신은 NORMAL로 저장한다. */
  @Test
  void storesPeriodTypeFromTrialToPaid() throws Exception {
    Long userId = createUser("rc-trial");

    postWebhook(
            WEBHOOK_SECRET,
            event(
                "INITIAL_PURCHASE",
                userId,
                BASE_EVENT_TIMESTAMP_MS,
                Map.of("period_type", "TRIAL")))
        .andExpect(status().isOk());
    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
    assertThat(subscriptionPeriodType(userId)).isEqualTo("TRIAL");

    postWebhook(
            WEBHOOK_SECRET,
            event(
                "RENEWAL",
                userId,
                BASE_EVENT_TIMESTAMP_MS + 1_000,
                Map.of("period_type", "NORMAL", "is_trial_conversion", "true")))
        .andExpect(status().isOk());
    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
    assertThat(subscriptionPeriodType(userId)).isEqualTo("NORMAL");
  }

  /** 프리미엄이 꺼지면 기간 종류를 비운다. */
  @Test
  void clearsPeriodTypeWhenPremiumTurnsOff() throws Exception {
    Long userId = createUser("rc-trial-expire");
    postWebhook(
            WEBHOOK_SECRET,
            event(
                "INITIAL_PURCHASE",
                userId,
                BASE_EVENT_TIMESTAMP_MS,
                Map.of("period_type", "TRIAL")))
        .andExpect(status().isOk());

    postWebhook(
            WEBHOOK_SECRET,
            event(
                "EXPIRATION",
                userId,
                BASE_EVENT_TIMESTAMP_MS + 1_000,
                Map.of("period_type", "TRIAL")))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("EXPIRED");
    assertThat(subscriptionPeriodType(userId)).isNull();
  }

  /** 알 수 없는 period_type은 기간 종류만 비우고 상태 갱신은 그대로 진행한다. */
  @Test
  void storesNullPeriodTypeForUnknownValue() throws Exception {
    Long userId = createUser("rc-period-unknown");

    postWebhook(
            WEBHOOK_SECRET,
            event(
                "INITIAL_PURCHASE",
                userId,
                BASE_EVENT_TIMESTAMP_MS,
                Map.of("period_type", "SOMETHING_NEW")))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
    assertThat(subscriptionPeriodType(userId)).isNull();
  }

  /** 이미 반영한 이벤트보다 오래된 이벤트가 뒤늦게 도착하면 상태는 무시하되 이력에는 남긴다. */
  @Test
  void ignoresStaleEvent() throws Exception {
    Long userId = createUser("rc-stale");
    postWebhook(WEBHOOK_SECRET, event("EXPIRATION", userId, BASE_EVENT_TIMESTAMP_MS + 5_000))
        .andExpect(status().isOk());

    postWebhook(WEBHOOK_SECRET, event("RENEWAL", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("EXPIRED");
    assertThat(subscriptionEvents(userId))
        .extracting(row -> row.get("type"))
        .containsExactly("RENEWAL", "EXPIRATION");
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
    assertThat(subscriptionEvents(userId)).isEmpty();
    assertThat(subscriptionEvents(987_654_321L)).isEmpty();
  }

  /** 구매 이벤트는 상품 ID와 스토어를 프로필에 저장하고, 프리미엄이 꺼지면 둘 다 비운다. */
  @Test
  void storesProductAndStoreWhilePremiumAndClearsWhenOff() throws Exception {
    Long userId = createUser("rc-product");

    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());
    assertThat(subscriptionProductId(userId)).isEqualTo("landit_premium_monthly");
    assertThat(subscriptionStore(userId)).isEqualTo("APP_STORE");

    postWebhook(WEBHOOK_SECRET, event("CANCELLATION", userId, BASE_EVENT_TIMESTAMP_MS + 1_000))
        .andExpect(status().isOk());
    assertThat(subscriptionProductId(userId)).isEqualTo("landit_premium_monthly");
    assertThat(subscriptionStore(userId)).isEqualTo("APP_STORE");

    postWebhook(WEBHOOK_SECRET, event("EXPIRATION", userId, BASE_EVENT_TIMESTAMP_MS + 2_000))
        .andExpect(status().isOk());
    assertThat(subscriptionProductId(userId)).isNull();
    assertThat(subscriptionStore(userId)).isNull();
  }

  /** 알 수 없는 store 값은 스토어만 비우고 상태 갱신은 그대로 진행한다. */
  @Test
  void storesNullStoreForUnknownValue() throws Exception {
    Long userId = createUser("rc-store-unknown");
    String body =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "INITIAL_PURCHASE",
            "app_user_id": "%d",
            "store": "SOMETHING_NEW",
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """
            .formatted(UUID.randomUUID(), userId, BASE_EVENT_TIMESTAMP_MS, EXPIRATION_MS);

    postWebhook(WEBHOOK_SECRET, body).andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
    assertThat(subscriptionStore(userId)).isNull();
    assertThat(subscriptionEvents(userId))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.get("store")).isNull();
              assertThat(row.get("price")).isNull();
              assertThat(row.get("currency")).isNull();
            });
  }

  /** 결제 이력은 결제 통화 기준 금액, 스토어, 환경, 결제 시각, 만료 시각, 해지 사유를 웹훅 그대로 저장한다. */
  @Test
  void recordsEventHistoryFromWebhookFields() throws Exception {
    Long userId = createUser("rc-history");

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

    List<Map<String, Object>> events = subscriptionEvents(userId);
    assertThat(events).hasSize(2);
    Map<String, Object> purchase = events.get(0);
    assertThat(purchase.get("event_id")).isNotNull();
    assertThat(purchase.get("type")).isEqualTo("INITIAL_PURCHASE");
    assertThat(purchase.get("product_id")).isEqualTo("landit_premium_monthly");
    assertThat((BigDecimal) purchase.get("price")).isEqualByComparingTo("58500");
    assertThat(purchase.get("currency")).isEqualTo("KRW");
    assertThat(purchase.get("store")).isEqualTo("APP_STORE");
    assertThat(purchase.get("environment")).isEqualTo("SANDBOX");
    assertThat(purchase.get("cancel_reason")).isNull();
    assertThat(((Timestamp) purchase.get("occurred_at")).getTime())
        .isEqualTo(BASE_EVENT_TIMESTAMP_MS);
    assertThat(((Timestamp) purchase.get("expires_at")).getTime()).isEqualTo(EXPIRATION_MS);
    Map<String, Object> refund = events.get(1);
    assertThat(refund.get("type")).isEqualTo("CANCELLATION");
    assertThat(refund.get("cancel_reason")).isEqualTo("CUSTOMER_SUPPORT");
  }

  /** 결제 시각이 없으면 이벤트 생성 시각을 발생 시각으로 저장한다. */
  @Test
  void fallsBackToEventTimestampWhenPurchasedAtMissing() throws Exception {
    Long userId = createUser("rc-occurred-fallback");
    String body =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "EXPIRATION",
            "app_user_id": "%d",
            "event_timestamp_ms": %d
          }
        }
        """
            .formatted(UUID.randomUUID(), userId, BASE_EVENT_TIMESTAMP_MS + 7_000);

    postWebhook(WEBHOOK_SECRET, body).andExpect(status().isOk());

    assertThat(subscriptionEvents(userId))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(((Timestamp) row.get("occurred_at")).getTime())
                  .isEqualTo(BASE_EVENT_TIMESTAMP_MS + 7_000);
              assertThat(row.get("expires_at")).isNull();
            });
  }

  /** 같은 이벤트 ID가 다시 오면 이력을 한 번만 남기고 상태도 바꾸지 않는다. */
  @Test
  void ignoresDuplicateEventId() throws Exception {
    Long userId = createUser("rc-duplicate");
    String eventId = UUID.randomUUID().toString();
    String template =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "%s",
            "app_user_id": "%d",
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """;
    postWebhook(
            WEBHOOK_SECRET,
            template.formatted(
                eventId, "INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS, EXPIRATION_MS))
        .andExpect(status().isOk());

    postWebhook(
            WEBHOOK_SECRET,
            template.formatted(
                eventId, "EXPIRATION", userId, BASE_EVENT_TIMESTAMP_MS + 1_000, EXPIRATION_MS))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
    assertThat(subscriptionEvents(userId))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.get("event_id")).isEqualTo(eventId);
              assertThat(row.get("type")).isEqualTo("INITIAL_PURCHASE");
            });
  }

  /** 결제 실패와 플랜 변경 이벤트는 이력으로만 남기고 구독 상태는 바꾸지 않는다. */
  @Test
  void recordsBillingIssueAndProductChangeWithoutStatusChange() throws Exception {
    Long userId = createUser("rc-history-only");
    postWebhook(WEBHOOK_SECRET, event("INITIAL_PURCHASE", userId, BASE_EVENT_TIMESTAMP_MS))
        .andExpect(status().isOk());

    postWebhook(WEBHOOK_SECRET, event("BILLING_ISSUE", userId, BASE_EVENT_TIMESTAMP_MS + 1_000))
        .andExpect(status().isOk());
    postWebhook(WEBHOOK_SECRET, event("PRODUCT_CHANGE", userId, BASE_EVENT_TIMESTAMP_MS + 2_000))
        .andExpect(status().isOk());

    assertThat(subscriptionStatus(userId)).isEqualTo("ACTIVE");
    assertThat(subscriptionEvents(userId))
        .extracting(row -> row.get("type"))
        .containsExactly("INITIAL_PURCHASE", "BILLING_ISSUE", "PRODUCT_CHANGE");
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
            "store": "APP_STORE",
            "price": 39.5,
            "price_in_purchased_currency": 58500,
            "currency": "KRW",
            "purchased_at_ms": %d,
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d,
            "unknown_field": {"nested": true}%s
          }
        }
        """;
    return template.formatted(
        UUID.randomUUID(),
        type,
        userId,
        userId,
        userId,
        eventTimestampMs,
        eventTimestampMs,
        EXPIRATION_MS,
        extra);
  }

  private String subscriptionStatus(Long userId) {
    return jdbcTemplate.queryForObject(
        "select subscription_status from user_profile where id = ?", String.class, userId);
  }

  private String subscriptionPeriodType(Long userId) {
    return jdbcTemplate.queryForObject(
        "select subscription_period_type from user_profile where id = ?", String.class, userId);
  }

  private String subscriptionProductId(Long userId) {
    return jdbcTemplate.queryForObject(
        "select subscription_product_id from user_profile where id = ?", String.class, userId);
  }

  private String subscriptionStore(Long userId) {
    return jdbcTemplate.queryForObject(
        "select subscription_store from user_profile where id = ?", String.class, userId);
  }

  /** 사용자의 결제 이력을 발생 시각 오름차순으로 조회한다. */
  private List<Map<String, Object>> subscriptionEvents(Long userId) {
    return jdbcTemplate.queryForList(
        "select * from subscription_event where user_profile_id = ? order by occurred_at, id",
        userId);
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
