// 관리자 사용자 목록과 상세 조회 API를 검증한다.

package com.landit.landitbe.feature.admin;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/** 관리자 사용자 목록과 상세 조회 API를 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminUserApiIntegrationTests {

  private static final long TEST_CATEGORY_ID = 9820;
  private static final long FIRST_SCENARIO_ID = 9821;
  private static final long SECOND_SCENARIO_ID = 9822;
  private static final Instant TEST_INSTANT = Instant.parse("2026-08-08T15:00:00Z");

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void clearScenarioFixtures() {
    jdbcTemplate.update(
        "DELETE FROM user_scenario_access WHERE scenario_id IN (?, ?)",
        FIRST_SCENARIO_ID,
        SECOND_SCENARIO_ID);
    jdbcTemplate.update(
        "DELETE FROM user_scenario_progress WHERE scenario_id IN (?, ?)",
        FIRST_SCENARIO_ID,
        SECOND_SCENARIO_ID);
    jdbcTemplate.update(
        "DELETE FROM scenario_language_variant WHERE scenario_id IN (?, ?)",
        FIRST_SCENARIO_ID,
        SECOND_SCENARIO_ID);
    jdbcTemplate.update(
        "DELETE FROM scenario WHERE id IN (?, ?)", FIRST_SCENARIO_ID, SECOND_SCENARIO_ID);
    jdbcTemplate.update(
        "DELETE FROM category_language_variant WHERE category_id = ?", TEST_CATEGORY_ID);
    jdbcTemplate.update("DELETE FROM category WHERE id = ?", TEST_CATEGORY_ID);
  }

  @DisplayName("사용자 목록을 최신 가입순으로 페이지 조회한다.")
  @Test
  void listsUsersInLatestSignupOrderWithPagination() throws Exception {
    final String accessToken = loginAdmin();
    String olderUserKey = "admin-user-older-" + UUID.randomUUID();
    String latestUserKey = "admin-user-latest-" + UUID.randomUUID();
    login(olderUserKey, "오래된 사용자");
    login(latestUserKey, "최근 사용자");
    jdbcTemplate.update(
        "UPDATE user_profile SET created_at = ?, updated_at = ? WHERE email = ?",
        "2098-01-01 00:00:00",
        "2098-01-01 00:00:00",
        olderUserKey + "@example.com");
    jdbcTemplate.update(
        "UPDATE user_profile SET created_at = ?, updated_at = ? WHERE email = ?",
        "2099-01-01 00:00:00",
        "2099-01-01 00:00:00",
        latestUserKey + "@example.com");

    mockMvc
        .perform(
            get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(2))
        .andExpect(jsonPath("$.data.items[0].email").value(latestUserKey + "@example.com"))
        .andExpect(jsonPath("$.data.items[0].nickname").value("최근 사용자"))
        .andExpect(jsonPath("$.data.items[0].role").value("USER"))
        .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.items[1].email").value(olderUserKey + "@example.com"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(2));
  }

  @DisplayName("사용자 상세에서 프로필과 학습 요약을 조회한다.")
  @Test
  void returnsProfileAndLearningSummaryForUserDetail() throws Exception {
    final String accessToken = loginAdmin();
    String userKey = "admin-user-detail-" + UUID.randomUUID();
    login(userKey, "상세 사용자");
    long userProfileId = userProfileId(userKey);
    seedScenarioContent();
    grantScenarioAccess(userProfileId, FIRST_SCENARIO_ID);
    jdbcTemplate.update(
        "UPDATE user_scenario_access SET granted_at = ? WHERE user_profile_id = ? "
            + "AND scenario_id = ?",
        "2026-08-07 10:00:00",
        userProfileId,
        FIRST_SCENARIO_ID);
    insertLearningSummary(userProfileId);

    mockMvc
        .perform(
            get("/api/v1/admin/users/{userProfileId}", userProfileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userProfileId").value(userProfileId))
        .andExpect(jsonPath("$.data.email").value(userKey + "@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("상세 사용자"))
        .andExpect(jsonPath("$.data.role").value("USER"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.targetLocale").value("EN"))
        .andExpect(jsonPath("$.data.baseLocale").value("KR"))
        .andExpect(jsonPath("$.data.currentLevel").value(1))
        .andExpect(jsonPath("$.data.pushPermissionStatus").value("NOT_DETERMINED"))
        .andExpect(jsonPath("$.data.learningSummary.completedScenarioCount").value(1))
        .andExpect(
            jsonPath("$.data.learningSummary.currentScenario.scenarioId").value(SECOND_SCENARIO_ID))
        .andExpect(
            jsonPath("$.data.learningSummary.currentScenario.scenarioTitle").value("두 번째 시나리오"))
        .andExpect(jsonPath("$.data.learningSummary.currentScenario.displayOrder").value(9822))
        .andExpect(
            jsonPath("$.data.learningSummary.currentScenario.dailyScenarioType").value("NEW"))
        .andExpect(jsonPath("$.data.learningSummary.currentStreakDays").value(3))
        .andExpect(jsonPath("$.data.learningSummary.lastLearningDate").value("2026-08-08"));
  }

  @DisplayName("학습 기록이 없는 사용자는 기본 학습 요약을 반환한다.")
  @Test
  void returnsDefaultLearningSummaryWhenUserHasNoLearningHistory() throws Exception {
    String accessToken = loginAdmin();
    String userKey = "admin-user-no-learning-history-" + UUID.randomUUID();
    login(userKey, "학습 기록 없는 사용자");
    long userProfileId = userProfileId(userKey);

    mockMvc
        .perform(
            get("/api/v1/admin/users/{userProfileId}", userProfileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.learningSummary.completedScenarioCount").value(0))
        .andExpect(jsonPath("$.data.learningSummary.currentScenario").value(nullValue()))
        .andExpect(jsonPath("$.data.learningSummary.currentStreakDays").value(0))
        .andExpect(jsonPath("$.data.learningSummary.lastLearningDate").value(nullValue()));
  }

  @DisplayName("존재하지 않는 사용자 상세 조회는 찾을 수 없음으로 응답한다.")
  @Test
  void returnsNotFoundForMissingUser() throws Exception {
    String accessToken = loginAdmin();

    mockMvc
        .perform(
            get("/api/v1/admin/users/{userProfileId}", 999999999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("USER_PROFILE_NOT_FOUND"));
  }

  @DisplayName("사용자 조회의 인증·권한·페이지 파라미터 오류를 거부한다.")
  @Test
  void rejectsUnauthenticatedNonAdminAndInvalidPageRequests() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    String accessToken = login("admin-user-normal-" + UUID.randomUUID(), "일반 사용자");

    mockMvc
        .perform(
            get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
    String adminAccessToken = loginAdmin();
    mockMvc
        .perform(
            get("/api/v1/admin/users")
                .param("size", "51")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
        .andExpect(status().isBadRequest());
  }

  @DisplayName("관리자 사용자 목록과 상세 API를 OpenAPI 문서에 노출한다.")
  @Test
  void documentsAdminUserApis() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.summary").exists())
        .andExpect(jsonPath("$.paths['/api/v1/admin/users/{userProfileId}'].get.summary").exists());
  }

  @DisplayName("관리자 사용자와 로그인 응답의 OpenAPI required 및 nullable 계약을 노출한다.")
  @Test
  void documentsAdminUserAndLoginResponseContracts() throws Exception {
    String schemas = "$.components.schemas.";

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(schemas + "AdminUserListItem").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserListResponse.properties.items.items.$ref")
                .value("#/components/schemas/AdminUserListItem"))
        .andExpect(jsonPath(schemas + "AdminUserListResponse.required[?(@ == 'items')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListResponse.required[?(@ == 'page')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListResponse.required[?(@ == 'size')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListResponse.required[?(@ == 'hasNext')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserListItem.required[?(@ == 'userProfileId')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListItem.required[?(@ == 'email')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListItem.required[?(@ == 'nickname')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListItem.required[?(@ == 'role')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListItem.required[?(@ == 'status')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListItem.required[?(@ == 'createdAt')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserListItem.properties.email.type[1]").value("null"))
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'userProfileId')]")
                .exists())
        .andExpect(jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'email')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'nickname')]").exists())
        .andExpect(jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'role')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'status')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'targetLocale')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'baseLocale')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'learningLevel')]")
                .exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'currentLevel')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'aiTutorId')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'pushPermissionStatus')]")
                .exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'createdAt')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'updatedAt')]").exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.required[?(@ == 'learningSummary')]")
                .exists())
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.properties.email.type[1]").value("null"))
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.properties.learningLevel.type[1]")
                .value("null"))
        .andExpect(
            jsonPath(schemas + "AdminUserDetailResponse.properties.aiTutorId.type[1]")
                .value("null"))
        .andExpect(
            jsonPath(schemas + "LearningSummary.required[?(@ == 'completedScenarioCount')]")
                .exists())
        .andExpect(
            jsonPath(schemas + "LearningSummary.required[?(@ == 'currentScenario')]").exists())
        .andExpect(
            jsonPath(schemas + "LearningSummary.required[?(@ == 'currentStreakDays')]").exists())
        .andExpect(
            jsonPath(schemas + "LearningSummary.properties.currentScenario.type[1]").value("null"))
        .andExpect(
            jsonPath(schemas + "LearningSummary.required[?(@ == 'lastLearningDate')]").exists())
        .andExpect(
            jsonPath(schemas + "LearningSummary.properties.lastLearningDate.type[1]").value("null"))
        .andExpect(jsonPath(schemas + "CurrentScenario.required[?(@ == 'scenarioId')]").exists())
        .andExpect(jsonPath(schemas + "CurrentScenario.required[?(@ == 'scenarioTitle')]").exists())
        .andExpect(jsonPath(schemas + "CurrentScenario.required[?(@ == 'displayOrder')]").exists())
        .andExpect(
            jsonPath(schemas + "CurrentScenario.required[?(@ == 'dailyScenarioType')]").exists())
        .andExpect(jsonPath(schemas + "AuthTokenResponse.required[?(@ == 'tokenType')]").exists())
        .andExpect(jsonPath(schemas + "AuthTokenResponse.required[?(@ == 'accessToken')]").exists())
        .andExpect(
            jsonPath(schemas + "AuthTokenResponse.required[?(@ == 'accessTokenExpiresIn')]")
                .exists())
        .andExpect(
            jsonPath(schemas + "AuthTokenResponse.required[?(@ == 'refreshToken')]").exists())
        .andExpect(
            jsonPath(schemas + "AuthTokenResponse.required[?(@ == 'refreshTokenExpiresIn')]")
                .exists())
        .andExpect(jsonPath(schemas + "AuthTokenResponse.required[?(@ == 'user')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.required[?(@ == 'userId')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.required[?(@ == 'nickname')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.required[?(@ == 'email')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.required[?(@ == 'provider')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.required[?(@ == 'newUser')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.required[?(@ == 'role')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.required[?(@ == 'status')]").exists())
        .andExpect(jsonPath(schemas + "AuthUserResponse.properties.email.type[1]").value("null"));
  }

  private String loginAdmin() throws Exception {
    String userKey = "admin-user-admin-" + UUID.randomUUID();
    String accessToken = login(userKey, "관리자");
    jdbcTemplate.update(
        "UPDATE user_profile SET role = 'ADMIN' WHERE id = ?", userProfileId(userKey));
    return accessToken;
  }

  private String login(String userKey, String nickname) throws Exception {
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
                            .formatted(userKey, userKey, nickname, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }

  private long userProfileId(String userKey) {
    return jdbcTemplate.queryForObject(
        "SELECT id FROM user_profile WHERE email = ?", Long.class, userKey + "@example.com");
  }

  private void seedScenarioContent() {
    jdbcTemplate.update(
        """
        INSERT INTO category (id, display_order, status, created_at, updated_at)
        VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        TEST_CATEGORY_ID,
        TEST_CATEGORY_ID);
    jdbcTemplate.update(
        """
        INSERT INTO category_language_variant (
            category_id, base_locale, name, created_at, updated_at)
        VALUES (
            ?, 'KR', '관리자 사용자 테스트', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        TEST_CATEGORY_ID);
    insertScenario(FIRST_SCENARIO_ID, 9821, "첫 번째 시나리오");
    insertScenario(SECOND_SCENARIO_ID, 9822, "두 번째 시나리오");
  }

  private void insertScenario(long scenarioId, int displayOrder, String title) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at)
        VALUES (
            ?, ?, 'tutor', 'EASY', 'USER', 1, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        TEST_CATEGORY_ID,
        displayOrder);
    jdbcTemplate.update(
        """
        INSERT INTO scenario_language_variant (
            scenario_id, target_locale, base_locale, title, briefing,
            conversation_goal, status, created_at, updated_at)
        VALUES (
            ?, 'EN', 'KR', ?, '설명', '목표', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        title);
  }

  private void grantScenarioAccess(long userProfileId, long scenarioId) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_access (
            user_profile_id, scenario_id, target_locale, granted_at, created_at, updated_at)
        VALUES (?, ?, 'EN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userProfileId,
        scenarioId);
  }

  private void insertLearningSummary(long userProfileId) {
    jdbcTemplate.update(
        """
        INSERT INTO user_learning_activity_summary (
            user_profile_id, total_session_count, completed_scenario_count,
            completed_free_talk_count, completed_review_count, total_turn_count,
            total_study_seconds, learned_expression_count, average_native_score,
            current_streak_days, longest_streak_days, last_activity_date,
            created_at, updated_at)
        VALUES (
            ?, 3, 1, 0, 0, 3, 300, 0, NULL, 3, 3, '2026-08-08',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userProfileId);
  }

  @TestConfiguration
  static class FixedClockConfiguration {

    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
    }
  }
}
