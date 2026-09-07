// 유료 구독 도입 시점이 설정되지 않았을 때 대화 완료 여부가 항상 false인지 검증한다.

package com.landit.landitbe.feature.subscription;

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

/** 유료 구독 도입 시점이 설정되지 않았을 때 대화 완료 여부가 항상 false인지 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.subscription.revenuecat.webhook-authorization="
          + UserSubscriptionLaunchUnsetIntegrationTests.WEBHOOK_SECRET
    })
class UserSubscriptionLaunchUnsetIntegrationTests {

  static final String WEBHOOK_SECRET = "test-revenuecat-webhook-secret";

  private static final long CATEGORY_ID = 7_002L;
  private static final long SCENARIO_ID = 7_102L;

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 도입 시점이 비어 있으면 완료 이력이 있어도 대화 완료로 응답하지 않는다. */
  @Test
  void returnsFalseWhenLaunchedAtIsNotConfigured() throws Exception {
    String userKey = "subscription-launch-unset";
    String accessToken = login(userKey);
    seedClearedScenario(userKey);

    mockMvc
        .perform(
            get("/api/v1/me/subscription")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.conversationCompletedSinceLaunch").value(false));
  }

  private void seedClearedScenario(String userKey) {
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
        VALUES (?, 'KR', '도입 전 테스트 카테고리', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
    Long userId =
        jdbcTemplate.queryForObject(
            "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_progress (
            user_profile_id, scenario_id, target_locale, status, completed_count,
            first_cleared_at, last_cleared_at, last_played_at, created_at, updated_at
        )
        VALUES (?, ?, 'EN', 'CLEARED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        SCENARIO_ID);
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
