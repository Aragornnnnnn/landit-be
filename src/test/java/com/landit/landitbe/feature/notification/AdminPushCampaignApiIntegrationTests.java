// 관리자 푸시 API의 권한, 멱등성, 대상 격리와 불변 원문 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.notification.service.AdminPushCampaignService;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.annotation.Transactional;

/** 실제 인증과 관리자 권한 필터를 통과하는 캠페인 API 통합 검증이다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminPushCampaignApiIntegrationTests {

  private static final String BASE = "/api/v1/admin/push-campaigns";
  private static final String KEY_HEADER = "Idempotency-Key";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;
  @Autowired private AdminPushCampaignService campaignService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void createsNormalizedImmutableCampaignAndDeduplicatesCreation() throws Exception {
    Login admin = loginAdmin();
    String key = UUID.randomUUID().toString();
    String id = create(admin, key, " 공지 ");

    mockMvc
        .perform(
            post(BASE)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .header(KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content("공지")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(id))
        .andExpect(jsonPath("$.data.title").value("공지"))
        .andExpect(jsonPath("$.data.status").value("DRAFT"))
        .andExpect(jsonPath("$.data.broadcast").value(nullValue()));
    assertThat(auditCount(admin.userId(), "PUSH_CAMPAIGN_CREATED")).isEqualTo(1);

    mockMvc
        .perform(
            post(BASE)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .header(KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content("다른 제목")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_CONFLICT"));
  }

  @Test
  void rejectsAnonymousAndNonAdminOnReadAndWrite() throws Exception {
    mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post(BASE)
                .header(KEY_HEADER, "anonymous")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content("공지")))
        .andExpect(status().isUnauthorized());
    Login user = login(false);

    mockMvc
        .perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(user)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post(BASE)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .header(KEY_HEADER, "ordinary-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content("공지")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post(BASE + "/{id}/test-runs", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .header(KEY_HEADER, "ordinary-user-test"))
        .andExpect(status().isForbidden());
  }

  @Test
  void previewsTokenUnitsAndReturnsTheSameBroadcastForDifferentAdministrators() throws Exception {
    Login admin = loginAdmin();
    final Login anotherAdmin = loginAdmin();
    String id = create(admin, "preview-create", "공지");
    final JsonNode before = preview(admin, id);
    insertToken(admin.userId(), "ACTIVE");
    insertToken(admin.userId(), "ACTIVE");
    insertToken(admin.userId(), "REVOKED");

    JsonNode after = preview(admin, id);
    assertThat(after.path("estimatedUserCount").asLong())
        .isEqualTo(before.path("estimatedUserCount").asLong() + 1);
    assertThat(after.path("estimatedTokenCount").asLong())
        .isEqualTo(before.path("estimatedTokenCount").asLong() + 2);
    assertThat(after.path("estimatedAt").asText()).isNotBlank();
    String runId = start(admin, id, "send", "first-send");

    mockMvc
        .perform(
            post(BASE + "/{id}/send", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(anotherAdmin))
                .header(KEY_HEADER, "different-admin-and-key"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.id").value(runId))
        .andExpect(jsonPath("$.data.mode").value("BROADCAST"))
        .andExpect(jsonPath("$.data.targetTokenCount").value(nullValue()));
    assertThat(auditCount(admin.userId(), "PUSH_CAMPAIGN_SENT")).isEqualTo(1);
    assertThat(auditCount(anotherAdmin.userId(), "PUSH_CAMPAIGN_SENT")).isZero();
  }

  @Test
  void testRunCapturesOnlyAuthenticatedAdministratorsActiveTokens() throws Exception {
    Login admin = loginAdmin();
    final Login anotherUser = login(false);
    insertToken(admin.userId(), "ACTIVE");
    insertToken(admin.userId(), "ACTIVE");
    insertToken(admin.userId(), "REVOKED");
    insertToken(anotherUser.userId(), "ACTIVE");
    String id = create(admin, "test-create", "원문 제목");
    String runId = start(admin, id, "test-runs", "self-test");

    var work = campaignService.claim(UUID.fromString(runId), 1).orElseThrow();
    campaignService.prepare(work);
    campaignService.finish(work);

    mockMvc
        .perform(
            get(BASE + "/{id}/runs/{runId}", id, runId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.mode").value("TEST"))
        .andExpect(jsonPath("$.data.targetUserCount").value(1))
        .andExpect(jsonPath("$.data.targetTokenCount").value(2))
        .andExpect(jsonPath("$.data.pendingCount").value(2));
    assertThat(
            jdbcTemplate.queryForList(
                "SELECT DISTINCT user_profile_id FROM admin_push_target WHERE run_id = ?",
                Long.class,
                UUID.fromString(runId)))
        .containsExactly(admin.userId());
    assertThat(start(admin, id, "test-runs", "self-test")).isEqualTo(runId);
    assertThat(auditCount(admin.userId(), "PUSH_CAMPAIGN_TESTED")).isEqualTo(1);

    String broadcastId = start(admin, id, "send", "after-test");
    assertThat(broadcastId).isNotEqualTo(runId);
    mockMvc
        .perform(get(BASE + "/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("원문 제목"))
        .andExpect(jsonPath("$.data.broadcast.id").value(broadcastId))
        .andExpect(jsonPath("$.data.broadcast.targetTokenCount").value(nullValue()))
        .andExpect(jsonPath("$.data.tests.length()").value(1));
    assertThat(start(admin, id, "test-runs", "self-test")).isEqualTo(runId);
    mockMvc
        .perform(
            post(BASE + "/{id}/test-runs", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .header(KEY_HEADER, "new-test-after-send"))
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsNewTestWithinTenSecondsButAllowsRetryingTheSameKey() throws Exception {
    Login admin = loginAdmin();
    String id = create(admin, "rate-create", "공지");
    String runId = start(admin, id, "test-runs", "rate-first");
    assertThat(start(admin, id, "test-runs", "rate-first")).isEqualTo(runId);

    mockMvc
        .perform(
            post(BASE + "/{id}/test-runs", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .header(KEY_HEADER, "rate-second"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void rejectsWrongCampaignRunAndDoesNotExposeMutationEndpoints() throws Exception {
    Login admin = loginAdmin();
    String firstId = create(admin, "first-create", "첫 공지");
    String secondId = create(admin, "second-create", "둘째 공지");
    String runId = start(admin, firstId, "send", "send-first");

    mockMvc
        .perform(
            get(BASE + "/{id}/runs/{runId}", secondId, runId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post(BASE + "/{id}/runs/{runId}/resume", secondId, runId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put(BASE + "/{id}", firstId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(content("변경 시도")))
        .andExpect(status().isMethodNotAllowed());
    mockMvc
        .perform(
            patch(BASE + "/{id}", firstId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(content("변경 시도")))
        .andExpect(status().isMethodNotAllowed());
    mockMvc
        .perform(get(BASE + "/{id}", firstId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("첫 공지"));
  }

  @Test
  void rejectsMissingKeyUnsafeLinkAndInvalidPage() throws Exception {
    Login admin = loginAdmin();
    mockMvc
        .perform(
            post(BASE)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(content("공지")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post(BASE)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .header(KEY_HEADER, "invalid-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("title", "공지", "body", "본문", "deepLink", "/%252fevil.example"))))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(admin)).param("size", "51"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void documentsCampaignCreateAndRunEndpoints() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/push-campaigns'].post.summary").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/test-runs'].post.summary")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/send'].post.summary")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/admin/push-campaigns/{campaignId}"
                        + "/runs/{runId}/resume'].post.summary")
                .exists());
  }

  private String create(Login admin, String key, String title) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                    .header(KEY_HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content(title)))
            .andExpect(status().isCreated())
            .andReturn();
    return data(result).path("id").asText();
  }

  private JsonNode preview(Login admin, String id) throws Exception {
    return data(
        mockMvc
            .perform(
                get(BASE + "/{id}/audience-preview", id)
                    .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
            .andExpect(status().isOk())
            .andReturn());
  }

  private String start(Login admin, String id, String action, String key) throws Exception {
    return data(mockMvc
            .perform(
                post(BASE + "/{id}/" + action, id)
                    .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                    .header(KEY_HEADER, key))
            .andExpect(status().isAccepted())
            .andReturn())
        .path("id")
        .asText();
  }

  private String content(String title) throws Exception {
    return objectMapper.writeValueAsString(
        Map.of("title", title, "body", "본문", "deepLink", "/home"));
  }

  private JsonNode data(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
  }

  private long auditCount(long adminId, String action) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM admin_audit_log WHERE admin_user_profile_id = ? AND action = ?",
        Long.class,
        adminId,
        action);
  }

  private void insertToken(long userId, String status) {
    jdbcTemplate.update(
        """
        INSERT INTO user_push_token
          (user_profile_id, platform, expo_push_token, status, created_at, updated_at)
        VALUES (?, 'IOS', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        "ExponentPushToken[" + UUID.randomUUID() + "]",
        status);
  }

  private String bearer(Login login) {
    return "Bearer " + login.accessToken();
  }

  private Login loginAdmin() throws Exception {
    return login(true);
  }

  private Login login(boolean admin) throws Exception {
    String userKey = "admin-push-api-" + UUID.randomUUID();
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
                          "idToken":"%s|%s@example.com|관리자 푸시 테스트|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    long userId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM user_profile WHERE email = ?", Long.class, userKey + "@example.com");
    if (admin) {
      jdbcTemplate.update("UPDATE user_profile SET role = 'ADMIN' WHERE id = ?", userId);
    }
    entityManager.clear();
    return new Login(data(result).path("accessToken").asText(), userId);
  }

  private record Login(String accessToken, long userId) {}
}
