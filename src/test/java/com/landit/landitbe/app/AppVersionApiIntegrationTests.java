// 앱 버전 확인 API의 플랫폼 분리, 업데이트 정책, 오류 응답을 검증한다.
package com.landit.landitbe.app;

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

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AppVersionApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM app_version");
    }

    @Test
    void iosAndAndroidPoliciesAreQueriedIndependentlyWithoutAuthentication() throws Exception {
        insertPolicy("IOS", "1.4.0", 18, 15);
        insertPolicy("ANDROID", "2.0.0", 30, 25);

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updateType").value("SOFT"))
                .andExpect(jsonPath("$.data.latestVersionName").value("1.4.0"))
                .andExpect(jsonPath("$.data.latestBuildNumber").value(18))
                .andExpect(jsonPath("$.data.minimumSupportedBuildNumber").value(15))
                .andExpect(jsonPath("$.data.reason").value("IOS 업데이트를 권장합니다."))
                .andExpect(jsonPath("$.data.releasedAt").value("2026-06-09T12:00:00"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "ANDROID")
                        .param("buildNumber", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("NONE"))
                .andExpect(jsonPath("$.data.latestVersionName").value("2.0.0"))
                .andExpect(jsonPath("$.data.reason").value(nullValue()));
    }

    @Test
    void buildBelowMinimumReturnsForce() throws Exception {
        insertPolicy("IOS", "1.4.0", 18, 15);

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("FORCE"))
                .andExpect(jsonPath("$.data.reason").value("IOS 강제 업데이트가 필요합니다."));
    }

    @Test
    void supportedBuildBelowLatestReturnsSoft() throws Exception {
        insertPolicy("IOS", "1.4.0", 18, 15);

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("SOFT"))
                .andExpect(jsonPath("$.data.reason").value("IOS 업데이트를 권장합니다."));
    }

    @Test
    void latestOrHigherBuildReturnsNone() throws Exception {
        insertPolicy("IOS", "1.4.0", 18, 15);

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("NONE"))
                .andExpect(jsonPath("$.data.reason").value(nullValue()));

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("NONE"));
    }

    @Test
    void missingActivePolicyReturnsConfigurationError() throws Exception {
        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "18"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("APP_VERSION_POLICY_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.error.message")
                        .value("앱 버전 정책이 올바르게 설정되지 않았습니다."));
    }

    @Test
    void invalidPlatformReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "WINDOWS")
                        .param("buildNumber", "18"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void invalidOrMissingBuildNumberReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingPlatformReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("buildNumber", "18"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void openApiDocumentsAppVersionCheckContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.summary")
                        .value("앱 버전 업데이트 확인"))
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.parameters.length()")
                        .value(2))
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses['200']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses['400']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses['500']")
                        .exists());
    }

    private void insertPolicy(
            String platform,
            String versionName,
            long buildNumber,
            long minimumSupportedBuildNumber
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO app_version (
                            platform, version_name, build_number, minimum_supported_build_number,
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
                buildNumber,
                minimumSupportedBuildNumber,
                platform + " 강제 업데이트가 필요합니다.",
                platform + " 업데이트를 권장합니다."
        );
    }
}
