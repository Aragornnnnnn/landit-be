// 사용자 구독 상태·결제 이력 조회 API의 인증·계약, 웹훅 반영 결과, 도입 이후 대화 완료 판정을 검증한다.

package com.landit.landitbe.feature.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.profile.preference.service.ProfilePreferenceService;
import com.landit.landitbe.feature.profile.subscription.service.ProfileDiscountOfferService;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 사용자 구독 상태·결제 이력 조회 API의 인증·계약, 웹훅 반영 결과, 도입 이후 대화 완료 판정을 검증한다. */
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

  private static final long EVENT_TIMESTAMP_MS = 4_000_000_000_000L;
  private static final long EXPIRATION_MS = EVENT_TIMESTAMP_MS + 30L * 24 * 60 * 60 * 1000;
  private static final long CATEGORY_ID = 7_001L;
  private static final long SCENARIO_ID = 7_101L;
  private static final LocalDateTime BEFORE_LAUNCH = LocalDateTime.of(2026, 8, 31, 23, 59, 59);
  private static final LocalDateTime AFTER_LAUNCH = LocalDateTime.of(2026, 9, 1, 0, 0, 0);

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ProfileDiscountOfferService discountOffers;

  @Autowired private Clock clock;

  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired private ProfilePreferenceService preferences;

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
  @DisplayName("구독 이력이 없는 사용자는 NONE 상태에 프리미엄이 꺼진 것으로 조회된다.")
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
        .andExpect(jsonPath("$.data.isTrial").value(false))
        .andExpect(jsonPath("$.data.periodType").isEmpty())
        .andExpect(jsonPath("$.data.expiresAt").isEmpty())
        .andExpect(jsonPath("$.data.conversationCompletedSinceLaunch").value(false))
        .andExpect(jsonPath("$.data.productId").isEmpty())
        .andExpect(jsonPath("$.data.store").isEmpty());
  }

  /** 웹훅으로 무료 체험 구매가 반영되면 ACTIVE 상태, 체험 중(isTrial), TRIAL 기간 종류, 만료 시각이 조회된다. */
  @DisplayName("웹훅으로 무료 체험 구매가 반영되면 ACTIVE 상태, 체험 중(isTrial), TRIAL 기간 종류, 만료 시각이 조회된다.")
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
            "product_id": "com.saynow.app.premium.yearly",
            "store": "PLAY_STORE",
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """
            .formatted(UUID.randomUUID(), userId, EVENT_TIMESTAMP_MS, EXPIRATION_MS);
    postWebhook(body);

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.data.premium").value(true))
        .andExpect(jsonPath("$.data.isTrial").value(true))
        .andExpect(jsonPath("$.data.periodType").value("TRIAL"))
        .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
        .andExpect(jsonPath("$.data.productId").value("com.saynow.app.premium.yearly"))
        .andExpect(jsonPath("$.data.store").value("PLAY_STORE"));
  }

  /**
   * 대시보드에서 부여한 프로모션 권한(NON_RENEWING_PURCHASE)은 프리미엄이 켜지지만 무료 체험이 아니므로 isTrial은 false이고 periodType은
   * PROMOTIONAL로 조회된다. 앱은 이 값으로 체험 종료 결제 경고를 보내지 않는다.
   */
  @DisplayName("프로모션 구독은 프리미엄으로 조회하되 무료 체험으로 표시하지 않는다.")
  @Test
  void returnsPromotionalSubscriptionAsPremiumButNotTrial() throws Exception {
    String userKey = "subscription-promotional";
    String accessToken = login(userKey);
    Long userId = userIdOf(userKey);
    String body =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "NON_RENEWING_PURCHASE",
            "app_user_id": "%d",
            "product_id": "rc_promo_premium_monthly",
            "period_type": "PROMOTIONAL",
            "store": "PROMOTIONAL",
            "environment": "PRODUCTION",
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """
            .formatted(UUID.randomUUID(), userId, EVENT_TIMESTAMP_MS, EXPIRATION_MS);
    postWebhook(body);

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.data.premium").value(true))
        .andExpect(jsonPath("$.data.isTrial").value(false))
        .andExpect(jsonPath("$.data.periodType").value("PROMOTIONAL"))
        .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
        .andExpect(jsonPath("$.data.productId").value("rc_promo_premium_monthly"))
        .andExpect(jsonPath("$.data.store").value("PROMOTIONAL"));
  }

  /** 결제 이력은 발생 시각 내림차순으로 내려오고, 결제 없는 이벤트의 price는 0이다. */
  @DisplayName("결제 이력은 발생 시각 내림차순으로 내려오고, 결제 없는 이벤트의 price는 0이다.")
  @Test
  void returnsSubscriptionEventsNewestFirst() throws Exception {
    String userKey = "subscription-events";
    final String accessToken = login(userKey);
    Long userId = userIdOf(userKey);
    String renewalId = postOutOfOrderSubscriptionEvents(userId);

    mockMvc
        .perform(
            get("/api/v1/me/subscription/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(3))
        .andExpect(jsonPath("$.data[0].eventId").value(renewalId))
        .andExpect(jsonPath("$.data[0].type").value("RENEWAL"))
        .andExpect(jsonPath("$.data[0].productId").value("com.saynow.app.premium.yearly"))
        .andExpect(jsonPath("$.data[0].periodType").value("NORMAL"))
        .andExpect(jsonPath("$.data[0].price").value(58500))
        .andExpect(jsonPath("$.data[0].currency").value("KRW"))
        .andExpect(jsonPath("$.data[0].store").value("APP_STORE"))
        .andExpect(jsonPath("$.data[0].environment").value("PRODUCTION"))
        .andExpect(jsonPath("$.data[0].cancelReason").isEmpty())
        .andExpect(jsonPath("$.data[0].occurredAt").isNotEmpty())
        .andExpect(jsonPath("$.data[0].expiresAt").isNotEmpty())
        .andExpect(jsonPath("$.data[1].type").value("CANCELLATION"))
        .andExpect(jsonPath("$.data[1].price").value(0))
        .andExpect(jsonPath("$.data[1].currency").isEmpty())
        .andExpect(jsonPath("$.data[1].cancelReason").value("UNSUBSCRIBE"))
        .andExpect(jsonPath("$.data[2].type").value("INITIAL_PURCHASE"))
        .andExpect(jsonPath("$.data[2].periodType").value("TRIAL"))
        .andExpect(jsonPath("$.data[2].price").value(0));
  }

  /** 결제 이력은 페이지 없이 최근 50개까지만 내려준다. */
  @DisplayName("결제 이력은 페이지 없이 최근 50개까지만 내려준다.")
  @Test
  void limitsSubscriptionEventsToFifty() throws Exception {
    String userKey = "subscription-events-limit";
    String accessToken = login(userKey);
    Long userId = userIdOf(userKey);
    IntStream.range(0, 55)
        .forEach(
            index ->
                jdbcTemplate.update(
                    """
                    INSERT INTO subscription_event (
                        event_id, user_profile_id, type, occurred_at, created_at
                    )
                    VALUES (?, ?, 'RENEWAL', ?, CURRENT_TIMESTAMP)
                    """,
                    "limit-" + userId + "-" + index,
                    userId,
                    AFTER_LAUNCH.plusDays(index)));

    mockMvc
        .perform(
            get("/api/v1/me/subscription/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(50))
        .andExpect(jsonPath("$.data[0].eventId").value("limit-" + userId + "-54"))
        .andExpect(jsonPath("$.data[49].eventId").value("limit-" + userId + "-5"));
  }

  /** 이력이 없는 사용자는 빈 목록을 받는다. */
  @DisplayName("이력이 없는 사용자는 빈 목록을 받는다.")
  @Test
  void returnsEmptySubscriptionEventsForNewUser() throws Exception {
    String accessToken = login("subscription-events-empty");

    mockMvc
        .perform(
            get("/api/v1/me/subscription/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  /** 도입 시점 이후에 시나리오를 끝까지 완료한 사용자는 대화 완료로 조회된다. */
  @DisplayName("도입 시점 이후에 시나리오를 끝까지 완료한 사용자는 대화 완료로 조회된다.")
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

  /**
   * 도입 후 시나리오를 완료한 무료 사용자에게도 구독 조회 응답이 새 시나리오 시작을 허용한다고 알리는지 검증한다.
   *
   * <p>기존 정책에서는 도입 후 대화를 완료한 무료 사용자에게 canStartScenario가 false였고, 앱은 이 값으로 시작 전 페이월을 띄웠다. 시나리오 대화가
   * 무료가 되면서 완료 이력과 관계없이 canStartScenario는 true여야 하고, conversationCompletedSinceLaunch는 완료 여부를 그대로
   * 알리며, 무료 상태로 시나리오를 시작한 적이 없어 freeScenarioSessionId는 null이어야 한다.
   */
  @DisplayName("도입 후 완료 이력이 있는 무료 사용자도 구독 응답에서 시나리오 시작을 허용한다.")
  @Test
  void keepsScenarioStartOpenForFreeUserAfterCompletionSinceLaunch() throws Exception {
    String userKey = "subscription-start-open";
    String accessToken = login(userKey);
    insertScenarioProgress(userIdOf(userKey), "CLEARED", AFTER_LAUNCH);

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.premium").value(false))
        .andExpect(jsonPath("$.data.paymentEnabled").value(true))
        .andExpect(jsonPath("$.data.conversationCompletedSinceLaunch").value(true))
        .andExpect(jsonPath("$.data.canStartScenario").value(true))
        .andExpect(
            jsonPath("$.data.freeScenarioSessionId").value(org.hamcrest.Matchers.nullValue()));
  }

  /** 도입 시점 전에만 완료한 기존 사용자는 대화 완료로 보지 않는다. */
  @DisplayName("도입 시점 전에만 완료한 기존 사용자는 대화 완료로 보지 않는다.")
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
  @DisplayName("시작만 하고 끝까지 완료하지 않은 시나리오는 대화 완료로 보지 않는다.")
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
  @DisplayName("인증되지 않은 사용자는 구독 상태를 조회할 수 없다.")
  @Test
  void rejectsUnauthenticatedSubscriptionRequest() throws Exception {
    mockMvc.perform(post("/api/v1/me/paywall/dismiss")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/me/subscription")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/me/subscription/events")).andExpect(status().isUnauthorized());
  }

  /** OpenAPI 문서에 구독 상태·결제 이력 조회 API와 새 필드를 Subscription 태그로 공개한다. */
  @DisplayName("OpenAPI 문서에 구독 상태·결제 이력 조회 API와 새 필드를 Subscription 태그로 공개한다.")
  @Test
  void openApiDocsDescribeSubscriptionApi() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/me/paywall/dismiss'].post.responses['401']").exists())
        .andExpect(
            jsonPath("$.components.schemas.UserSubscriptionResponse.properties.promo").exists())
        .andExpect(
            jsonPath("$.components.schemas.UserSubscriptionResponse.properties.price").exists())
        .andExpect(
            jsonPath("$.components.schemas.UserSubscriptionResponse.properties.currency").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.tags[0]").value("Subscription"))
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/me/subscription'].get.responses['401']").exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.UserSubscriptionResponse.properties"
                        + ".conversationCompletedSinceLaunch")
                .exists())
        .andExpect(
            jsonPath("$.components.schemas.UserSubscriptionResponse.properties.isTrial").exists())
        .andExpect(
            jsonPath("$.components.schemas.UserSubscriptionResponse.properties.trial")
                .doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.UserSubscriptionResponse.properties.productId").exists())
        .andExpect(
            jsonPath("$.components.schemas.UserSubscriptionResponse.properties.store").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/me/subscription/events'].get.tags[0]").value("Subscription"))
        .andExpect(
            jsonPath("$.paths['/api/v1/me/subscription/events'].get.responses['401']").exists())
        .andExpect(
            jsonPath("$.components.schemas.SubscriptionEventResponse.properties.environment")
                .exists());
  }

  private void postWebhook(String body) throws Exception {
    mockMvc
        .perform(
            post("/webhooks/revenuecat")
                .header(HttpHeaders.AUTHORIZATION, WEBHOOK_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
  }

  /** 운영 환경 App Store 연간 상품 이벤트 본문을 만든다. extraJson은 뒤에 쉼표를 붙인 JSON 필드 조각이다. */
  private static String webhookEvent(
      String eventId, String type, Long userId, long eventTimestampMs, String extraJson) {
    String template =
        """
        {
          "api_version": "1.0",
          "event": {
            "id": "%s",
            "type": "%s",
            "app_user_id": "%d",
            "product_id": "com.saynow.app.premium.yearly",
            "store": "APP_STORE",
            "environment": "PRODUCTION",
            %s
            "event_timestamp_ms": %d,
            "expiration_at_ms": %d
          }
        }
        """;
    return template.formatted(eventId, type, userId, extraJson, eventTimestampMs, EXPIRATION_MS);
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

  @Test
  @DisplayName("조회는 할인을 만들지 않고 최초 이탈만 5분을 부여하며 재조회·재로그인에도 유지한다.")
  void grantsPromoOnceAndSharesItWithSubscription() throws Exception {
    String token = login("promo-once");
    long userId = userIdOf("promo-once");
    subscription(token)
        .andExpect(jsonPath("$.data.promo").isEmpty())
        .andExpect(jsonPath("$.data.price").isEmpty())
        .andExpect(jsonPath("$.data.currency").isEmpty());
    assertThat(storedPromoExpiry(userId)).isNull();
    JsonNode first = dismissPromo(token);
    assertThat(first.path("remainingSeconds").asInt()).isEqualTo(300);
    assertThat(first.path("newUser").asBoolean()).isTrue();
    String expiresAt = first.path("expiresAt").asText();
    assertThat(dismissPromo(login("promo-once")).path("expiresAt").asText()).isEqualTo(expiresAt);
    subscription(token)
        .andExpect(jsonPath("$.data.promo.expiresAt").value(expiresAt))
        .andExpect(jsonPath("$.data.promo.newUser").value(true));
    jdbcTemplate.update(
        "UPDATE user_profile SET created_at = ? WHERE id = ?",
        LocalDateTime.now(clock).minusDays(30),
        userId);
    assertThat(dismissPromo(token).path("newUser").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("만료된 할인은 조회와 재이탈 모두 null이며 기존 기록을 유지한다.")
  void neverRestartsExpiredPromo() throws Exception {
    String token = login("promo-expired");
    long userId = userIdOf("promo-expired");
    dismissPromo(token);
    LocalDateTime expired = LocalDateTime.now(clock).minusMinutes(1).withNano(0);
    jdbcTemplate.update(
        "UPDATE user_profile SET discount_offer_expires_at = ? WHERE id = ?", expired, userId);
    assertThat(dismissPromo(token).isNull()).isTrue();
    subscription(token).andExpect(jsonPath("$.data.promo").isEmpty());
    assertThat(storedPromoExpiry(userId)).isEqualTo(expired);
  }

  @Test
  @DisplayName("기존 사용자도 첫 이탈 할인을 받지만 신규 혜택 라벨은 받지 않는다.")
  void grantsOldUserWithoutNewUserLabel() throws Exception {
    String token = login("promo-old");
    jdbcTemplate.update(
        "UPDATE user_profile SET created_at = ? WHERE id = ?",
        LocalDateTime.now(clock).minusDays(30),
        userIdOf("promo-old"));
    assertThat(dismissPromo(token).path("newUser").asBoolean()).isFalse();
    assertThat(storedPromoExpiry(userIdOf("promo-old"))).isNotNull();
  }

  @Test
  @DisplayName("무료 체험·해지 예약을 포함한 프리미엄은 할인을 소진하지 않고 기존 할인도 숨긴다.")
  void suppressesPromoForPremiumWithoutConsumingIt() throws Exception {
    String token = login("promo-premium");
    long userId = userIdOf("promo-premium");
    jdbcTemplate.update(
        """
        UPDATE user_profile SET subscription_status = 'CANCELED', subscription_period_type = 'TRIAL',
          subscription_expires_at = ? WHERE id = ?
        """,
        LocalDateTime.now(clock).plusDays(1),
        userId);
    assertThat(dismissPromo(token).isNull()).isTrue();
    assertThat(storedPromoExpiry(userId)).isNull();
    jdbcTemplate.update(
        "UPDATE user_profile SET subscription_expires_at = ? WHERE id = ?",
        LocalDateTime.now(clock).minusDays(1),
        userId);
    JsonNode promo = dismissPromo(token);
    assertThat(promo.isObject()).isTrue();
    final LocalDateTime expiry = storedPromoExpiry(userId);
    jdbcTemplate.update(
        "UPDATE user_profile SET subscription_expires_at = ? WHERE id = ?",
        LocalDateTime.now(clock).plusDays(1),
        userId);
    assertThat(dismissPromo(token).isNull()).isTrue();
    subscription(token).andExpect(jsonPath("$.data.promo").isEmpty());
    assertThat(storedPromoExpiry(userId)).isEqualTo(expiry);
  }

  @Test
  @DisplayName("동시 최초 이탈 요청은 하나의 만료 시각만 저장하고 동일한 값을 반환한다.")
  void concurrentDismissalsKeepOneExpiry() throws Exception {
    login("promo-concurrent");
    long userId = userIdOf("promo-concurrent");
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<LocalDateTime> dismiss =
          () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
              throw new IllegalStateException("동시 요청 시작 대기 실패");
            }
            return discountOffers.dismiss(userId).expiresAt();
          };
      var first = executor.submit(dismiss);
      var second = executor.submit(dismiss);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      LocalDateTime expiry = first.get(10, TimeUnit.SECONDS);
      assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(expiry);
      assertThat(storedPromoExpiry(userId)).isEqualTo(expiry);
    }
  }

  @Test
  @DisplayName("결제 없는 후속 이벤트 50건 이상을 건너뛰고 최신 결제액을 사용자별로 조회한다.")
  void returnsLatestPaymentBeyondEventListLimit() throws Exception {
    final String token = login("promo-price");
    long userId = userIdOf("promo-price");
    insertPayment(userId, "INITIAL_PURCHASE", "NORMAL", 58500, "KRW", 0);
    insertPayment(userId, "RENEWAL", "NORMAL", 94800, "KRW", 1);
    insertPayment(userId, "PRODUCT_CHANGE", "INTRO", 14900, "KRW", 2);
    insertPayment(userId, "RENEWAL", "NORMAL", 12900, null, 2);
    for (int index = 0; index < 55; index++) {
      insertPayment(userId, "CANCELLATION", "NORMAL", 99999, "USD", 3 + index);
    }
    insertPayment(userId, "EXPIRATION", "NORMAL", null, null, 60);
    insertPayment(userId, "INITIAL_PURCHASE", "TRIAL", 0, "KRW", 61);
    insertPayment(userId, "INITIAL_PURCHASE", "PROMOTIONAL", 100, "KRW", 62);
    insertPayment(userId, "RENEWAL", "NORMAL", null, "USD", 63);
    login("promo-other-price");
    insertPayment(userIdOf("promo-other-price"), "RENEWAL", "NORMAL", 777, "USD", 64);
    subscription(token)
        .andExpect(jsonPath("$.data.price").value(12900))
        .andExpect(jsonPath("$.data.currency").isEmpty());
  }

  @Test
  @DisplayName("웹훅의 실제 결제액·통화와 Play 베이스 플랜 ID를 그대로 반환한다.")
  void preservesPlayBasePlanAndWebhookPrice() throws Exception {
    String token = login("promo-play-price");
    long userId = userIdOf("promo-play-price");
    postWebhook(
        webhookEvent(
            UUID.randomUUID().toString(),
            "INITIAL_PURCHASE",
            userId,
            EVENT_TIMESTAMP_MS,
            "\"period_type\":\"TRIAL\",\"price_in_purchased_currency\":0,\"currency\":\"KRW\","));
    subscription(token).andExpect(jsonPath("$.data.price").isEmpty());
    String event =
        webhookEvent(
                UUID.randomUUID().toString(),
                "RENEWAL",
                userId,
                EVENT_TIMESTAMP_MS + 1000,
                "\"period_type\":\"NORMAL\",\"price_in_purchased_currency\":58500,"
                    + "\"currency\":\"KRW\",")
            .replace("APP_STORE", "PLAY_STORE")
            .replace("com.saynow.app.premium.yearly", "com.saynow.app.premium.yearly:promo");
    postWebhook(event);
    subscription(token)
        .andExpect(jsonPath("$.data.productId").value("com.saynow.app.premium.yearly:promo"))
        .andExpect(jsonPath("$.data.price").value(58500))
        .andExpect(jsonPath("$.data.currency").value("KRW"));
  }

  @Test
  @DisplayName("할인 부여 전 읽은 억양 변경 트랜잭션은 할인 기록을 보존하고 재부여하지 않는다.")
  void preservesDiscountWhenStalePreferenceTransactionCommitsLater() throws Exception {
    final String token = login("promo-stale-preference");
    long userId = userIdOf("promo-stale-preference");
    var grantedExpiry = new AtomicReference<LocalDateTime>();
    try (var executor = Executors.newSingleThreadExecutor()) {
      new TransactionTemplate(transactionManager)
          .executeWithoutResult(
              status -> {
                preferences.updateAccentLocale(userId, AccentLocale.EN_GB);
                try {
                  grantedExpiry.set(
                      executor
                          .submit(() -> discountOffers.dismiss(userId).expiresAt())
                          .get(10, TimeUnit.SECONDS));
                } catch (Exception exception) {
                  throw new IllegalStateException(exception);
                }
              });
    }
    assertThat(grantedExpiry.get()).isNotNull();
    assertThat(storedPromoExpiry(userId)).isEqualTo(grantedExpiry.get());
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT discount_offer_new_user FROM user_profile WHERE id = ?",
                Boolean.class,
                userId))
        .isTrue();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT accent_locale FROM user_profile WHERE id = ?", String.class, userId))
        .isEqualTo("EN_GB");
    assertThat(LocalDateTime.parse(dismissPromo(token).path("expiresAt").asText()))
        .isEqualTo(grantedExpiry.get());
  }

  private void insertPayment(
      long userId, String type, String period, Integer price, String currency, int second) {
    jdbcTemplate.update(
        """
        INSERT INTO subscription_event
          (event_id, user_profile_id, type, period_type, price, currency, occurred_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """,
        UUID.randomUUID().toString(),
        userId,
        type,
        period,
        price,
        currency,
        AFTER_LAUNCH.plusSeconds(second));
  }

  private LocalDateTime storedPromoExpiry(long userId) {
    return jdbcTemplate.queryForObject(
        "SELECT discount_offer_expires_at FROM user_profile WHERE id = ?",
        LocalDateTime.class,
        userId);
  }

  private ResultActions subscription(String token) throws Exception {
    return mockMvc
        .perform(
            get("/api/v1/me/subscription").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
  }

  private JsonNode dismissPromo(String token) throws Exception {
    var result =
        mockMvc
            .perform(
                post("/api/v1/me/paywall/dismiss")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .path("data")
        .path("promo");
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

  private String postOutOfOrderSubscriptionEvents(Long userId) throws Exception {
    String renewalId = UUID.randomUUID().toString();
    postWebhook(
        webhookEvent(
            UUID.randomUUID().toString(),
            "INITIAL_PURCHASE",
            userId,
            EVENT_TIMESTAMP_MS,
            """
            "period_type": "TRIAL", "price": 0, "price_in_purchased_currency": 0, "currency": "KRW",
            "purchased_at_ms": %d,
            """
                .formatted(EVENT_TIMESTAMP_MS)));
    postWebhook(
        webhookEvent(
            renewalId,
            "RENEWAL",
            userId,
            EVENT_TIMESTAMP_MS + 2_000,
            """
            "period_type": "NORMAL", "price": 39.5, "price_in_purchased_currency": 58500.0,
            "currency": "KRW", "purchased_at_ms": %d,
            """
                .formatted(EVENT_TIMESTAMP_MS + 2_000)));
    postWebhook(
        webhookEvent(
            UUID.randomUUID().toString(),
            "CANCELLATION",
            userId,
            EVENT_TIMESTAMP_MS + 1_000,
            """
            "cancel_reason": "UNSUBSCRIBE",
            """));

    return renewalId;
  }
}
