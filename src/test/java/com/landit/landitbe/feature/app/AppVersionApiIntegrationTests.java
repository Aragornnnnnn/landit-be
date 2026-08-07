// 앱 버전명 기준 업데이트 정책과 오류 응답을 검증한다.

package com.landit.landitbe.feature.app;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 앱 버전명 기준 업데이트 정책과 오류 응답을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AppVersionApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  /** 각 테스트 전에 플랫폼별 정책을 비운다. */
  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM app_version");
  }

  /** IOS와 Android 정책은 서로 독립적으로 최신 및 최소 지원 버전을 판단한다. */
  @Test
  void iosAndAndroidPoliciesAreQueriedIndependentlyWithoutAuthentication() throws Exception {
    insertPolicy("IOS", "1.4.0", "1.2.0", 18);
    insertPolicy("ANDROID", "2.0.0", "1.5.0", 30);

    check("IOS", "1.3.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.updateType").value("SOFT"))
        .andExpect(jsonPath("$.data.latestVersionName").value("1.4.0"))
        .andExpect(jsonPath("$.data.minimumSupportedVersionName").value("1.2.0"))
        .andExpect(jsonPath("$.data.reason").value("IOS 업데이트를 권장합니다."))
        .andExpect(jsonPath("$.data.releasedAt").value("2026-06-09T12:00:00"))
        .andExpect(jsonPath("$.error").value(nullValue()));

    check("ANDROID", "2.0.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("NONE"))
        .andExpect(jsonPath("$.data.latestVersionName").value("2.0.0"))
        .andExpect(jsonPath("$.data.reason").value(nullValue()));
  }

  /** 최소 지원 버전보다 낮은 앱은 빌드 번호와 무관하게 강제 업데이트를 받는다. */
  @Test
  void versionBelowMinimumReturnsForce() throws Exception {
    insertPolicy("IOS", "1.3.0", "1.1.0", 100);

    check("IOS", "1.0.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("FORCE"))
        .andExpect(jsonPath("$.data.reason").value("IOS 강제 업데이트가 필요합니다."));
  }

  /** 최소 지원 이상이면서 최신 버전보다 낮은 앱은 소프트 업데이트를 받는다. */
  @Test
  void supportedVersionBelowLatestReturnsSoft() throws Exception {
    insertPolicy("IOS", "1.3.0", "1.1.0", 100);

    check("IOS", "1.1.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("SOFT"))
        .andExpect(jsonPath("$.data.reason").value("IOS 업데이트를 권장합니다."));
    check("IOS", "1.2.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("SOFT"));
  }

  /** Minor와 Patch 버전은 문자열이 아닌 숫자 순서로 비교한다. */
  @Test
  void versionNamesAreComparedNumerically() throws Exception {
    insertPolicy("IOS", "1.10.0", "1.9.0", 100);

    check("IOS", "1.9.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("SOFT"));
    check("IOS", "1.10.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("NONE"));
  }

  /** 최신 버전 이상 앱은 빌드 번호와 관계없이 업데이트가 필요 없다. */
  @Test
  void latestOrHigherVersionReturnsNone() throws Exception {
    insertPolicy("IOS", "1.3.0", "1.1.0", 100);

    check("IOS", "1.3.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("NONE"));
    check("IOS", "1.3.1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("NONE"));
  }

  /** 플랫폼 정책이 없으면 설정 오류를 반환한다. */
  @Test
  void missingPolicyReturnsConfigurationError() throws Exception {
    check("IOS", "1.0.0")
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").value(nullValue()))
        .andExpect(jsonPath("$.error.code").value("APP_VERSION_POLICY_NOT_CONFIGURED"));
  }

  /** 형식이 맞지 않는 앱 버전명은 요청 오류로 거절한다. */
  @Test
  void invalidVersionNameReturnsValidationFailed() throws Exception {
    check("IOS", "1.0")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  /** 빌드 번호 없이도 앱 버전 업데이트 확인을 수행한다. */
  @Test
  void buildNumberIsNotRequiredForVersionCheck() throws Exception {
    insertPolicy("IOS", "1.3.0", "1.1.0", 100);

    mockMvc
        .perform(
            get("/api/v1/app-versions/check")
                .param("platform", "IOS")
                .param("versionName", "1.0.0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updateType").value("FORCE"));
  }

  /** 빌드 번호가 없는 요청에서 버전명이 누락되면 요청 오류로 거절한다. */
  @Test
  void missingVersionNameWithoutBuildNumberReturnsValidationFailed() throws Exception {
    mockMvc
        .perform(get("/api/v1/app-versions/check").param("platform", "IOS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  /** 공개 OpenAPI 문서는 플랫폼과 앱 버전명 요청값만 노출한다. */
  @Test
  void openApiDocumentsAppVersionCheckContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/v1/app-versions/check'].get.summary").value("앱 버전 업데이트 확인"))
        .andExpect(
            jsonPath("$.paths['/api/v1/app-versions/check'].get.parameters.length()").value(2))
        .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses['400']").exists());
  }

  /** 앱 버전 확인 요청을 만든다. */
  private org.springframework.test.web.servlet.ResultActions check(
      String platform, String versionName) throws Exception {
    return mockMvc.perform(
        get("/api/v1/app-versions/check")
            .param("platform", platform)
            .param("versionName", versionName));
  }

  /** 최신과 최소 지원 버전명이 지정된 플랫폼 정책을 추가한다. */
  private void insertPolicy(
      String platform, String versionName, String minimumSupportedVersionName, long buildNumber) {
    jdbcTemplate.update(
        """
        INSERT INTO app_version (
            platform, version_name, minimum_supported_version_name, build_number,
            force_update_reason, soft_update_reason, release_note, active,
            released_at, created_at
        )
        VALUES (
            ?, ?, ?, ?, ?, ?, NULL, TRUE,
            TIMESTAMP '2026-06-09 12:00:00', CURRENT_TIMESTAMP
        )
        """,
        platform,
        versionName,
        minimumSupportedVersionName,
        buildNumber,
        platform + " 강제 업데이트가 필요합니다.",
        platform + " 업데이트를 권장합니다.");
  }
}
