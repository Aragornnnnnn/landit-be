// Dev 전용 일반 푸시 테스트 API의 인증과 직접 발송 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.service.NotificationDispatchService;
import com.landit.landitbe.feature.notification.service.SendPushNotificationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Dev 전용 일반 푸시 테스트 API의 인증과 직접 발송 계약을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Import(PushNotificationTestApiIntegrationTests.TestNotificationDispatchConfiguration.class)
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.notification.test-api-enabled=true"
    })
class PushNotificationTestApiIntegrationTests {

  private static final String TEST_ENDPOINT = "/api/v1/internal/test/push";

  @Autowired private MockMvc mockMvc;

  @Autowired private NotificationDispatchService notificationDispatchService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 각 테스트 전에 직접 발송 Service의 호출 기록을 비운다. */
  @BeforeEach
  void resetNotificationDispatchService() {
    reset(notificationDispatchService);
  }

  /** 인증된 요청은 현재 사용자에게 보낼 일반 테스트 알림을 즉시 발송한다. */
  @Test
  void publishesTestNotificationForAuthenticatedRequest() throws Exception {
    JsonNode loginData = login("push-test-api");

    mockMvc
        .perform(
            post(TEST_ENDPOINT)
                .header(
                    HttpHeaders.AUTHORIZATION, "Bearer " + loginData.get("accessToken").asText())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

    ArgumentCaptor<SendPushNotificationCommand> commandCaptor =
        ArgumentCaptor.forClass(SendPushNotificationCommand.class);
    verify(notificationDispatchService).send(commandCaptor.capture());
    SendPushNotificationCommand command = commandCaptor.getValue();
    assertThat(command.eventId()).isNotBlank();
    assertThat(command.userProfileId()).isEqualTo(loginData.get("user").get("userId").asLong());
    assertThat(command.notificationType()).isEqualTo(NotificationType.TEST_NOTIFICATION);
    assertThat(command.title()).isEqualTo("Landit 알림 테스트");
    assertThat(command.body()).isEqualTo("푸시 알림이 정상적으로 도착했어요.");
    assertThat(command.deepLink()).isEqualTo("/home");
  }

  /** 인증되지 않은 요청은 dev 테스트 API를 실행할 수 없다. */
  @Test
  void rejectsRequestWithoutAccessToken() throws Exception {
    mockMvc.perform(post(TEST_ENDPOINT)).andExpect(status().isUnauthorized());

    verifyNoInteractions(notificationDispatchService);
  }

  /** 테스트 식별자를 사용하는 가짜 소셜 로그인으로 접근 토큰을 발급한다. */
  private JsonNode login(String userKey) throws Exception {
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
    return body.get("data");
  }

  /** 테스트용 직접 발송 Service Bean을 등록한다. */
  @TestConfiguration
  static class TestNotificationDispatchConfiguration {

    @Bean
    NotificationDispatchService notificationDispatchService() {
      return org.mockito.Mockito.mock(NotificationDispatchService.class);
    }
  }
}
