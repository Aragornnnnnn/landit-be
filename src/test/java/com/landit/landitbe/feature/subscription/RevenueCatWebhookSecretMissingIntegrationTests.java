// 웹훅 Authorization 설정값이 비어 있으면 RevenueCat 웹훅을 모두 거절하는지 검증한다.

package com.landit.landitbe.feature.subscription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** 웹훅 Authorization 설정값이 비어 있으면 RevenueCat 웹훅을 모두 거절하는지 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class RevenueCatWebhookSecretMissingIntegrationTests {

  @Autowired private MockMvc mockMvc;

  /** 설정값이 비어 있으면 어떤 Authorization 헤더로도 웹훅을 받지 않는다. */
  @Test
  void rejectsEveryWebhookWhenSecretIsNotConfigured() throws Exception {
    mockMvc
        .perform(
            post("/webhooks/revenuecat")
                .header(HttpHeaders.AUTHORIZATION, "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"api_version":"1.0","event":{"id":"e1","type":"INITIAL_PURCHASE","app_user_id":"1"}}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("WEBHOOK_UNAUTHORIZED"));
  }
}
