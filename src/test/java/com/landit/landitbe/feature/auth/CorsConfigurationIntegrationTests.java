// 인증 API의 CORS preflight 처리와 응답 정책을 검증한다.

package com.landit.landitbe.feature.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** 인증 API의 CORS preflight 처리와 응답 정책을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {"landit.cors.allowed-origins=https://web.landit.im"})
class CorsConfigurationIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @DisplayName("인증 API의 CORS 사전 요청에 허용 출처와 기본 설정을 적용한다.")
  @Test
  void preflightForAuthenticatedApiUsesConfiguredOriginAndCodeDefaults() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/me")
                .header(HttpHeaders.ORIGIN, "https://web.landit.im")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://web.landit.im"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("DELETE")))
        .andExpect(
            header()
                .string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
  }

  @DisplayName("설정된 웹 출처의 관리자 이메일 사전 요청에서 Idempotency-Key를 허용한다.")
  @Test
  void adminEmailPreflightAllowsIdempotencyKeyFromConfiguredWebOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/admin/notifications/email-tests")
                .header(HttpHeaders.ORIGIN, "https://web.landit.im")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                    "Authorization, Content-Type, Idempotency-Key"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://web.landit.im"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Idempotency-Key")));
  }
}
