// 관리자 앱 버전 정책 API의 권한, 등록, 활성 전환, 감사 기록을 검증한다.

package com.landit.landitbe.feature.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/** 관리자 앱 버전 정책 API의 권한, 등록, 활성 전환, 감사 기록을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminAppVersionApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 각 테스트 전에 앱 버전 정책을 비운다. */
  @BeforeEach
  void clearPolicies() {
    jdbcTemplate.update("delete from app_version");
  }

  /** 일반 로그인 사용자는 관리자 앱 버전 목록을 조회할 수 없다. */
  @Test
  void rejectsNonAdminAppVersionList() throws Exception {
    String accessToken = login("admin-app-version-denied", "일반 사용자");

    mockMvc
        .perform(
            get("/api/v1/admin/app-versions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  /** 관리자는 iOS와 Android 앱 버전 정책 목록을 조회할 수 있다. */
  @Test
  void listsAppVersionPoliciesForAdmin() throws Exception {
    insertPolicy("IOS", "1.0.0", 10, 8, true);
    insertPolicy("ANDROID", "2.0.0", 20, 18, true);
    String adminAccessToken = loginAdmin("admin-app-version-list", "관리자");

    mockMvc
        .perform(
            get("/api/v1/admin/app-versions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[?(@.platform == 'IOS')].buildNumber").value(10))
        .andExpect(jsonPath("$.data[?(@.platform == 'ANDROID')].buildNumber").value(20));
  }

  /** 관리자는 정책을 등록하고 같은 플랫폼의 기존 활성 정책을 새 정책으로 전환한다. */
  @Test
  void createsAndActivatesAppVersionWithAuditLog() throws Exception {
    insertPolicy("IOS", "1.0.0", 10, 8, true);
    String adminAccessToken = loginAdmin("admin-app-version-admin", "관리자");

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/admin/app-versions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.platform").value("IOS"))
            .andExpect(jsonPath("$.data.active").value(false))
            .andReturn();
    long appVersionId =
        objectMapper
            .readTree(createResult.getResponse().getContentAsByteArray())
            .get("data")
            .get("appVersionId")
            .asLong();

    mockMvc
        .perform(
            post("/api/v1/admin/app-versions/{appVersionId}/activate", appVersionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.active").value(true));

    mockMvc
        .perform(
            get("/api/v1/app-versions/check").param("platform", "IOS").param("buildNumber", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.latestBuildNumber").value(11));

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from app_version where platform = 'IOS' and active = true",
                Long.class))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from admin_audit_log where action = 'APP_VERSION_ACTIVATED'",
                Long.class))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                "select before_value from admin_audit_log where action = 'APP_VERSION_ACTIVATED' "
                    + "and target_id = ? order by id desc limit 1",
                String.class,
                appVersionId))
        .contains("active=false");
    assertThat(
            jdbcTemplate.queryForObject(
                "select after_value from admin_audit_log where action = 'APP_VERSION_ACTIVATED' "
                    + "and target_id = ? order by id desc limit 1",
                String.class,
                appVersionId))
        .contains("active=true");
  }

  /** 관리자 수정은 정책의 모든 변경값과 감사 기록의 전후 값을 함께 반영한다. */
  @Test
  void updatesAppVersionAndRecordsBeforeAndAfterValues() throws Exception {
    insertPolicy("ANDROID", "1.0.0", 10, 8, true);
    Long appVersionId =
        jdbcTemplate.queryForObject(
            "select id from app_version where platform = 'ANDROID' and build_number = 10",
            Long.class);
    String adminAccessToken = loginAdmin("admin-app-version-update", "관리자");

    mockMvc
        .perform(
            patch("/api/v1/admin/app-versions/{appVersionId}", appVersionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "versionName":"1.1.0",
                      "buildNumber":11,
                      "minimumSupportedBuildNumber":9,
                      "forceUpdateReason":"필수 업데이트",
                      "softUpdateReason":"권장 업데이트",
                      "releaseNote":"안정성 개선",
                      "releasedAt":"2026-07-30T09:00:00"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.versionName").value("1.1.0"))
        .andExpect(jsonPath("$.data.buildNumber").value(11))
        .andExpect(jsonPath("$.data.minimumSupportedBuildNumber").value(9))
        .andExpect(jsonPath("$.data.forceUpdateReason").value("필수 업데이트"))
        .andExpect(jsonPath("$.data.softUpdateReason").value("권장 업데이트"))
        .andExpect(jsonPath("$.data.releaseNote").value("안정성 개선"))
        .andExpect(jsonPath("$.data.releasedAt").value("2026-07-30T09:00:00"));

    assertThat(
            jdbcTemplate.queryForObject(
                "select before_value from admin_audit_log where action = 'APP_VERSION_UPDATED' "
                    + "and target_id = ? order by id desc limit 1",
                String.class,
                appVersionId))
        .contains("versionName=1.0.0", "buildNumber=10", "minimumSupportedBuildNumber=8");
    assertThat(
            jdbcTemplate.queryForObject(
                "select after_value from admin_audit_log where action = 'APP_VERSION_UPDATED' "
                    + "and target_id = ? order by id desc limit 1",
                String.class,
                appVersionId))
        .contains("versionName=1.1.0", "buildNumber=11", "minimumSupportedBuildNumber=9");
  }

  /** 관리자 앱 버전 등록은 1보다 작은 빌드 번호를 요청 오류로 거절한다. */
  @Test
  void rejectsInvalidBuildNumberBeforeDatabaseWrite() throws Exception {
    String adminAccessToken = loginAdmin("admin-app-version-invalid", "관리자");

    mockMvc
        .perform(
            post("/api/v1/admin/app-versions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "versionName":"1.0.0",
                      "buildNumber":0,
                      "minimumSupportedBuildNumber":0,
                      "releasedAt":"2026-07-29T10:00:00"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  /** 관리자 앱 버전 API는 OpenAPI 문서에 노출된다. */
  @Test
  void documentsAdminAppVersionApis() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/app-versions'].get.summary").exists())
        .andExpect(jsonPath("$.paths['/api/v1/admin/app-versions'].post.summary").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/app-versions/{appVersionId}'].patch.summary").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/app-versions/{appVersionId}/activate'].post.summary")
                .exists());
  }

  /** 테스트 식별자와 이름으로 가짜 소셜 로그인을 수행하고 access token을 반환한다. */
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
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .get("data")
        .get("accessToken")
        .asText();
  }

  /** 테스트 사용자를 로그인시키고 관리자 허용 목록에 추가한다. */
  private String loginAdmin(String userKey, String nickname) throws Exception {
    String accessToken = login(userKey, nickname);
    Long userProfileId =
        jdbcTemplate.queryForObject(
            "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
    jdbcTemplate.update(
        "insert into admin_account (user_profile_id, created_at) values (?, current_timestamp)",
        userProfileId);
    return accessToken;
  }

  /** 활성 여부가 지정된 기존 앱 버전 정책을 추가한다. */
  private void insertPolicy(
      String platform,
      String versionName,
      long buildNumber,
      long minimumSupportedBuildNumber,
      boolean active) {
    jdbcTemplate.update(
        """
        insert into app_version (
            platform, version_name, build_number, minimum_supported_build_number, active, released_at,
            created_at
        )
        values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        platform,
        versionName,
        buildNumber,
        minimumSupportedBuildNumber,
        active);
  }

  /** 새 앱 버전 정책 등록 요청 본문을 만든다. */
  private String requestBody() {
    return "{\"platform\":\"IOS\",\"versionName\":\"1.1.0\",\"buildNumber\":11,"
        + "\"minimumSupportedBuildNumber\":9,\"forceUpdateReason\":\"강제 업데이트\","
        + "\"softUpdateReason\":\"권장 업데이트\",\"releaseNote\":\"개선 사항\","
        + "\"releasedAt\":\"2026-07-29T10:00:00\"}";
  }
}
