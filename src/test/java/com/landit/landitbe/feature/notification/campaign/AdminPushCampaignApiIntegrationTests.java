// 관리자 푸시 API의 권한 경계와 OpenAPI 노출을 검증한다.

package com.landit.landitbe.feature.notification.campaign;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.landit.landitbe.shared.security.AuthUserPrincipal;
import org.junit.jupiter.api.DisplayName;
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
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

  @DisplayName("캠페인 목록의 기본 페이지와 페이지 정보를 반환한다.")
  @Test
  @org.springframework.transaction.annotation.Transactional
  void returnsDefaultCampaignPage() throws Exception {
    long adminId = insertCampaignAdmin();

    mockMvc
        .perform(get("/api/v1/admin/push-campaigns").with(user(new AuthUserPrincipal(adminId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalCount").isNumber())
        .andExpect(jsonPath("$.data.totalPages").isNumber())
        .andExpect(jsonPath("$.data.hasNext").isBoolean());
  }

  @DisplayName("예약 여부와 상태로 캠페인 목록을 조회한다.")
  @Test
  @org.springframework.transaction.annotation.Transactional
  void acceptsScheduledCampaignFilter() throws Exception {
    long adminId = insertCampaignAdmin();

    mockMvc
        .perform(
            get("/api/v1/admin/push-campaigns")
                .param("scheduled", "true")
                .param("status", "SCHEDULED")
                .with(user(new AuthUserPrincipal(adminId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isArray());
  }

  @DisplayName("잘못된 캠페인 조회 인자를 거부하고 이후 정상 요청은 처리한다.")
  @Test
  @org.springframework.transaction.annotation.Transactional
  void rejectsInvalidCampaignQueryAndAcceptsFollowingValidQuery() throws Exception {
    long adminId = insertCampaignAdmin();

    mockMvc
        .perform(
            get("/api/v1/admin/push-campaigns")
                .param("scheduled", "maybe")
                .with(user(new AuthUserPrincipal(adminId))))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            get("/api/v1/admin/push-campaigns")
                .param("status", "UNKNOWN")
                .with(user(new AuthUserPrincipal(adminId))))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            get("/api/v1/admin/push-campaigns")
                .param("page", "-1")
                .with(user(new AuthUserPrincipal(adminId))))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(get("/api/v1/admin/push-campaigns").with(user(new AuthUserPrincipal(adminId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isArray());
  }

  @DisplayName("관리자가 아닌 인증 사용자의 캠페인 조회와 예약을 거부한다.")
  @Test
  void rejectsAuthenticatedNonAdminForQueryAndSchedule() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/push-campaigns").with(user(new AuthUserPrincipal(Long.MAX_VALUE))))
        .andExpect(status().isForbidden());
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

  @DisplayName("캠페인 목록과 대상 조회 및 예약 변경에는 인증이 필요하다.")
  @Test
  void requiresCampaignAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/admin/push-campaigns")).andExpect(status().isUnauthorized());
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
  }

  @DisplayName("캠페인 관리의 경로와 요청 및 응답 계약을 OpenAPI에 노출한다.")
  @Test
  void publishesCampaignOpenApiContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/push-campaigns'].post.summary").exists())
        .andExpect(jsonPath("$.paths['/api/v1/admin/push-campaigns/schedules']").doesNotExist())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns'].get.parameters[*].name")
                .value(org.hamcrest.Matchers.hasItems("scheduled", "status", "page", "size")))
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

  private long insertCampaignAdmin() {
    final long adminId = 99462071L;
    jdbc.update(
        """
        insert into user_profile(id,nickname,target_locale,base_locale,current_level,
          push_permission_status,status,role,created_at,updated_at)
        values (?,'schedule-list','EN','KR',1,'NOT_DETERMINED','ACTIVE','ADMIN',
          CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
        """,
        adminId);
    return adminId;
  }
}
