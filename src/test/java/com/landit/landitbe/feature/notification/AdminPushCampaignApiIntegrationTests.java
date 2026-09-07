// 관리자 푸시 API의 권한 경계와 OpenAPI 노출을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
  void requiresAuthenticationAndPublishesOpenApiContract() throws Exception {
    mockMvc.perform(get("/api/v1/admin/push-campaigns")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/admin/push-campaigns'].post.summary").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/test'].post.summary")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/admin/push-campaigns/{campaignId}/send'].post.summary")
                .exists());
  }
}
