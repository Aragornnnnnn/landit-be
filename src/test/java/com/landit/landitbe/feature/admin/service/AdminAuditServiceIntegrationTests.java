// 관리자 쓰기 작업의 감사 기록 저장과 민감값 차단을 통합 검증한다.

package com.landit.landitbe.feature.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.admin.domain.AdminAction;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 관리자 쓰기 작업의 감사 기록 저장과 민감값 차단을 통합 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminAuditServiceIntegrationTests {

  @Autowired private AdminAuditService adminAuditService;

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 관리자가 변경한 대상과 이전·이후 값을 감사 로그에 저장한다. */
  @Test
  void recordsAdminWriteAuditLog() throws Exception {
    Long adminUserProfileId = loginAndFindUserProfileId("admin-audit-record");

    adminAuditService.record(
        adminUserProfileId,
        AdminAction.APP_VERSION_UPDATED,
        "APP_VERSION",
        "IOS",
        "{\"versionName\":\"1.0.0\"}",
        "{\"versionName\":\"1.1.0\"}");

    Map<String, Object> auditLog =
        jdbcTemplate.queryForMap(
            """
            select admin_user_profile_id, action, target_type, target_id, before_value, after_value
            from admin_audit_log
            where admin_user_profile_id = ?
            """,
            adminUserProfileId);
    assertThat(auditLog)
        .containsEntry("ADMIN_USER_PROFILE_ID", adminUserProfileId)
        .containsEntry("ACTION", "APP_VERSION_UPDATED")
        .containsEntry("TARGET_TYPE", "APP_VERSION")
        .containsEntry("TARGET_ID", "IOS")
        .containsEntry("BEFORE_VALUE", "{\"versionName\":\"1.0.0\"}")
        .containsEntry("AFTER_VALUE", "{\"versionName\":\"1.1.0\"}");
  }

  /** 인증 정보가 포함된 감사 값은 저장하지 않는다. */
  @Test
  void rejectsSensitiveValuesFromAuditLog() throws Exception {
    Long adminUserProfileId = loginAndFindUserProfileId("admin-audit-sensitive");

    assertThatThrownBy(
            () ->
                adminAuditService.record(
                    adminUserProfileId,
                    AdminAction.APP_VERSION_UPDATED,
                    "APP_VERSION",
                    "IOS",
                    null,
                    "Bearer never-store-this"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                adminAuditService.record(
                    adminUserProfileId,
                    AdminAction.APP_VERSION_UPDATED,
                    "Bearer never-store-this",
                    "IOS",
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class);

    Integer auditLogCount =
        jdbcTemplate.queryForObject(
            "select count(*) from admin_audit_log where admin_user_profile_id = ?",
            Integer.class,
            adminUserProfileId);
    assertThat(auditLogCount).isZero();
  }

  /** 테스트 식별자를 사용하는 가짜 소셜 로그인으로 사용자 프로필을 생성한다. */
  private Long loginAndFindUserProfileId(String userKey) throws Exception {
    String nonce = userKey + "-nonce";
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
    String accessToken = body.get("data").get("accessToken").asText();
    assertThat(accessToken).isNotBlank();
    return jdbcTemplate.queryForObject(
        "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
  }
}
