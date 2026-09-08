// 관리자 푸시 API의 권한 경계와 OpenAPI 노출을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 실제 보안 필터와 springdoc에서 관리자 캠페인 API를 확인한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AdminPushCampaignApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsAuthenticatedNonAdminForQueryAndSchedule() throws Exception {
    for (String path :
        java.util.List.of(
            "/audience-query",
            "/00000000-0000-0000-0000-000000000000/schedule",
            "/00000000-0000-0000-0000-000000000000/cancel-schedule")) {
      mockMvc
          .perform(
              post("/api/v1/admin/push-campaigns" + path)
                  .with(user(new AuthUserPrincipal(Long.MAX_VALUE))))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void requiresAuthenticationAndPublishesOpenApiContract() throws Exception {
    for (String path :
        java.util.List.of(
            "/audience-query",
            "/00000000-0000-0000-0000-000000000000/schedule",
            "/00000000-0000-0000-0000-000000000000/cancel-schedule")) {
      mockMvc
          .perform(post("/api/v1/admin/push-campaigns" + path))
          .andExpect(status().isUnauthorized());
    }
    mockMvc.perform(get("/api/v1/admin/push-campaigns")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/push-campaigns'].post.summary").exists())
        .andExpect(
            jsonPath("$.components.schemas.AdminPushCampaignRequest.properties.audienceType.enum")
                .value(org.hamcrest.Matchers.contains("ALL", "SELECTED")))
        .andExpect(jsonPath("$.paths['/api/v1/admin/push-campaigns/audience-query'].post").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/schedule'].post").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/cancel-schedule'].post")
                .exists())
        .andExpect(
            jsonPath("$.components.schemas.AdminPushCampaignRequest.properties.audienceSql")
                .exists())
        .andExpect(
            jsonPath("$.components.schemas.AdminPushCampaignView.properties.scheduledAt").exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.AdminPushCampaignRequest.properties"
                        + ".userProfileIds.maxItems")
                .doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.AdminPushCampaignView.properties.audienceType").exists())
        .andExpect(
            jsonPath("$.components.schemas.AdminPushCampaignView.properties.userProfileIds")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/test'].post.summary")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/send'].post.summary")
                .exists());
  }
}
