// Expo Push Token 상태 관리 API의 인증과 저장 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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

/** Expo Push Token 상태 관리 API의 인증과 저장 계약을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class ExpoPushTokenApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 인증된 사용자는 PUT 요청으로 Expo Push Token을 등록하고 비활성화할 수 있다. */
  @Test
  void upsertsAndRevokesExpoPushToken() throws Exception {
    String userKey = "expo-push-token-owner";
    String accessToken = login(userKey);
    String expoPushToken = "ExponentPushToken[upsert-and-revoke]";

    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"%s",
                      "enabled":true
                    }
                    """
                        .formatted(expoPushToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
    assertTokenStatus(expoPushToken, "ACTIVE");

    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"%s",
                      "enabled":false
                    }
                    """
                        .formatted(expoPushToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
    assertTokenStatus(expoPushToken, "REVOKED");

    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"ANDROID",
                      "expoPushToken":"%s",
                      "enabled":true
                    }
                    """
                        .formatted(expoPushToken)))
        .andExpect(status().isOk());
    assertTokenStatus(expoPushToken, "ACTIVE");
    assertTokenPlatform(expoPushToken, "ANDROID");
  }

  /** 다른 사용자는 본인 소유가 아닌 Expo Push Token을 비활성화할 수 없다. */
  @Test
  void doesNotRevokeAnotherUsersExpoPushToken() throws Exception {
    String ownerAccessToken = login("expo-push-token-real-owner");
    String otherAccessToken = login("expo-push-token-other-user");
    String expoPushToken = "ExponentPushToken[owner-only-revoke]";
    registerToken(ownerAccessToken, expoPushToken);

    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"%s",
                      "enabled":false
                    }
                    """
                        .formatted(expoPushToken)))
        .andExpect(status().isOk());

    assertTokenStatus(expoPushToken, "ACTIVE");
  }

  /** 인증되지 않은 요청은 Expo Push Token 상태를 변경할 수 없다. */
  @Test
  void rejectsUnauthenticatedExpoPushTokenUpdate() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"ExponentPushToken[unauthenticated]",
                      "enabled":true
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  /** 테스트 식별자로 가짜 소셜 로그인을 수행하고 access token을 반환한다. */
  private String login(String userKey) throws Exception {
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
                            .formatted(userKey, userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }

  /** 테스트 사용자의 Expo Push Token을 활성 상태로 등록한다. */
  private void registerToken(String accessToken, String expoPushToken) throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/expo-push-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "platform":"IOS",
                      "expoPushToken":"%s",
                      "enabled":true
                    }
                    """
                        .formatted(expoPushToken)))
        .andExpect(status().isOk());
  }

  /** Expo Push Token의 현재 저장 상태를 검증한다. */
  private void assertTokenStatus(String expoPushToken, String expectedStatus) {
    String actualStatus =
        jdbcTemplate.queryForObject(
            "select status from user_push_token where expo_push_token = ?",
            String.class,
            expoPushToken);
    org.assertj.core.api.Assertions.assertThat(actualStatus).isEqualTo(expectedStatus);
  }

  /** Expo Push Token의 현재 저장 플랫폼을 검증한다. */
  private void assertTokenPlatform(String expoPushToken, String expectedPlatform) {
    String actualPlatform =
        jdbcTemplate.queryForObject(
            "select platform from user_push_token where expo_push_token = ?",
            String.class,
            expoPushToken);
    org.assertj.core.api.Assertions.assertThat(actualPlatform).isEqualTo(expectedPlatform);
  }
}
