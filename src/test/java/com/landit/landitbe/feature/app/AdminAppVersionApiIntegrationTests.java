// 관리자 앱 버전 정책 조회와 플랫폼별 수정을 검증한다.

package com.landit.landitbe.feature.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 관리자 앱 버전 정책 조회와 플랫폼별 수정을 검증한다. */
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

  /** 관리자는 iOS와 Android의 단일 버전 정책을 조회할 수 있다. */
  @Test
  void listsSinglePolicyForEachPlatform() throws Exception {
    insertPolicy("IOS", "1.0.0", "1.0.0", 10);
    insertPolicy("ANDROID", "2.0.0", "1.8.0", 20);
    String adminAccessToken = loginAdmin("admin-app-version-list", "관리자");

    mockMvc
        .perform(
            get("/api/v1/admin/app-versions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(
            jsonPath("$.data[?(@.platform == 'IOS')].minimumSupportedVersionName").value("1.0.0"))
        .andExpect(
            jsonPath("$.data[?(@.platform == 'ANDROID')].minimumSupportedVersionName")
                .value("1.8.0"));
  }

  /** 관리자는 플랫폼을 기준으로 단일 정책과 감사 기록을 함께 수정한다. */
  @Test
  void updatesPlatformPolicyAndRecordsBeforeAndAfterValues() throws Exception {
    insertPolicy("ANDROID", "1.0.0", "1.0.0", 10);
    String adminAccessToken = loginAdmin("admin-app-version-update", "관리자");
    Long adminUserProfileId = findUserProfileId("admin-app-version-update");

    mockMvc
        .perform(
            patch("/api/v1/admin/app-versions/{platform}", "ANDROID")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody("1.2.0", "1.1.0", 11)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.versionName").value("1.2.0"))
        .andExpect(jsonPath("$.data.minimumSupportedVersionName").value("1.1.0"))
        .andExpect(jsonPath("$.data.buildNumber").value(11))
        .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
        .andExpect(jsonPath("$.data.updatedBy").value("관리자"));

    assertThat(
            jdbcTemplate.queryForObject(
                "select updated_by_user_profile_id from app_version where platform = 'ANDROID'",
                Long.class))
        .isEqualTo(adminUserProfileId);

    mockMvc
        .perform(
            get("/api/v1/app-versions/check")
                .param("platform", "ANDROID")
                .param("versionName", "1.0.0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("FORCE"));

    assertThat(
            jdbcTemplate.queryForObject(
                "select before_value from admin_audit_log where action = 'APP_VERSION_UPDATED' "
                    + "order by id desc limit 1",
                String.class))
        .contains("versionName=1.0.0", "minimumSupportedVersionName=1.0.0");
    assertThat(
            jdbcTemplate.queryForObject(
                "select after_value from admin_audit_log where action = 'APP_VERSION_UPDATED' "
                    + "order by id desc limit 1",
                String.class))
        .contains("versionName=1.2.0", "minimumSupportedVersionName=1.1.0");
  }

  /** 감사 이력이 없는 기존 앱 버전 정책은 생성 시각과 빈 수정자로 이관한다. */
  @Test
  void migratesPolicyWithoutAuditHistoryUsingCreatedAtAndNullModifier() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            "jdbc:h2:mem:app-version-migration-"
                + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration", "classpath:db/h2")
        .target("55")
        .load()
        .migrate();
    JdbcTemplate migrationJdbcTemplate = new JdbcTemplate(dataSource);
    migrationJdbcTemplate.update(
        """
        insert into app_version (
            platform, version_name, minimum_supported_version_name,
            build_number, active, released_at, created_at
        )
        values ('IOS', '1.0.0', '1.0.0', 10, true,
                timestamp '2026-01-02 03:04:05', timestamp '2026-01-02 03:04:05')
        """);

    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration", "classpath:db/h2")
        .load()
        .migrate();

    assertThat(
            migrationJdbcTemplate.queryForObject(
                "select updated_at from app_version where platform = 'IOS'", String.class))
        .startsWith("2026-01-02 03:04:05");
    assertThat(
            migrationJdbcTemplate.queryForObject(
                "select updated_by_user_profile_id from app_version where platform = 'IOS'",
                Long.class))
        .isNull();
  }

  /** 최소 지원 버전이 최신 버전보다 높으면 정책 수정을 거절한다. */
  @Test
  void rejectsPolicyWithMinimumVersionHigherThanLatestVersion() throws Exception {
    insertPolicy("IOS", "1.0.0", "1.0.0", 10);
    String adminAccessToken = loginAdmin("admin-app-version-invalid", "관리자");

    mockMvc
        .perform(
            patch("/api/v1/admin/app-versions/{platform}", "IOS")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody("1.1.0", "1.2.0", 11)))
        .andExpect(status().isBadRequest());
  }

  /** Major.Minor.Patch 형식이 아닌 관리자 버전명은 정책 수정을 거절한다. */
  @Test
  void rejectsPolicyWithInvalidVersionName() throws Exception {
    insertPolicy("IOS", "1.0.0", "1.0.0", 10);
    String adminAccessToken = loginAdmin("admin-app-version-format", "관리자");

    mockMvc
        .perform(
            patch("/api/v1/admin/app-versions/{platform}", "IOS")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody("1.1", "1.0.0", 11)))
        .andExpect(status().isBadRequest());
  }

  /** 등록과 활성 전환 경로는 관리자 OpenAPI에 노출되지 않는다. */
  @Test
  void documentsOnlyListAndPlatformUpdateApis() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/app-versions'].get.summary").exists())
        .andExpect(jsonPath("$.paths['/api/v1/admin/app-versions'].post").doesNotExist())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/app-versions/{platform}'].patch.summary").exists())
        .andExpect(jsonPath("$.paths['/api/v1/admin/app-versions/{appVersionId}']").doesNotExist())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/app-versions/{appVersionId}/activate']")
                .doesNotExist());
  }

  /** 관리자 앱 버전 응답의 OpenAPI required 및 nullable 계약을 노출한다. */
  @Test
  void documentsAdminAppVersionResponseContract() throws Exception {
    String responseSchema = "$.components.schemas.AdminAppVersionResponse";

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'appVersionId')]").exists())
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'platform')]").exists())
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'versionName')]").exists())
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'buildNumber')]").exists())
        .andExpect(
            jsonPath(responseSchema + ".required[?(@ == 'minimumSupportedVersionName')]").exists())
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'forceUpdateReason')]").exists())
        .andExpect(jsonPath(responseSchema + ".properties.forceUpdateReason.type[1]").value("null"))
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'softUpdateReason')]").exists())
        .andExpect(jsonPath(responseSchema + ".properties.softUpdateReason.type[1]").value("null"))
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'releaseNote')]").exists())
        .andExpect(jsonPath(responseSchema + ".properties.releaseNote.type[1]").value("null"))
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'active')]").exists())
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'releasedAt')]").exists())
        .andExpect(jsonPath(responseSchema + ".properties.releasedAt.type[1]").value("null"))
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'updatedAt')]").exists())
        .andExpect(jsonPath(responseSchema + ".required[?(@ == 'updatedBy')]").exists())
        .andExpect(jsonPath(responseSchema + ".properties.updatedBy.type[1]").value("null"));
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

  /** 테스트 사용자를 로그인시키고 관리자 역할을 부여한다. */
  private String loginAdmin(String userKey, String nickname) throws Exception {
    String accessToken = login(userKey, nickname);
    Long userProfileId = findUserProfileId(userKey);
    jdbcTemplate.update("update user_profile set role = 'ADMIN' where id = ?", userProfileId);
    return accessToken;
  }

  /** 테스트 사용자 이메일로 사용자 프로필 ID를 조회한다. */
  private Long findUserProfileId(String userKey) {
    return jdbcTemplate.queryForObject(
        "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
  }

  /** 플랫폼별 단일 앱 버전 정책을 추가한다. */
  private void insertPolicy(
      String platform, String versionName, String minimumSupportedVersionName, long buildNumber) {
    jdbcTemplate.update(
        """
        insert into app_version (
            platform, version_name, minimum_supported_version_name,
            build_number, active, released_at,
            created_at, updated_at
        )
        values (?, ?, ?, ?, true, current_timestamp, current_timestamp, current_timestamp)
        """,
        platform,
        versionName,
        minimumSupportedVersionName,
        buildNumber);
  }

  /** 관리자 정책 수정 요청 본문을 만든다. */
  private String requestBody(
      String versionName, String minimumSupportedVersionName, long buildNumber) {
    return ("{\"versionName\":\"%s\","
            + "\"minimumSupportedVersionName\":\"%s\","
            + "\"buildNumber\":%d,"
            + "\"forceUpdateReason\":\"필수 업데이트\","
            + "\"softUpdateReason\":\"권장 업데이트\","
            + "\"releaseNote\":\"안정성 개선\","
            + "\"releasedAt\":\"2026-07-30T09:00:00\"}")
        .formatted(versionName, minimumSupportedVersionName, buildNumber);
  }
}
