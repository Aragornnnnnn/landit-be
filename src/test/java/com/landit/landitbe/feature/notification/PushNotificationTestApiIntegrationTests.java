// Dev 전용 복습 리마인더 테스트 API의 인증과 Queue 발행 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.messaging.PushNotificationRequest;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/** Dev 전용 일반 푸시 테스트 API의 인증과 Queue 발행 계약을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Import(PushNotificationTestApiIntegrationTests.TestPushQueuePublisherConfiguration.class)
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough",
      "landit.notification.test-api-enabled=true"
    })
class PushNotificationTestApiIntegrationTests {

  private static final String TEST_ENDPOINT = "/api/v1/internal/test/push";

  @Autowired private MockMvc mockMvc;

  @Autowired private RecordingPushQueuePublisher pushQueuePublisher;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 각 테스트 전에 기록된 푸시 발행 요청을 비운다. */
  @BeforeEach
  void clearPublishedNotification() {
    pushQueuePublisher.clear();
  }

  /** 인증된 요청은 현재 사용자에게 보낼 일반 테스트 알림을 Push Queue에 즉시 발행한다. */
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

    PushNotificationRequest request = pushQueuePublisher.notificationRequest();
    assertThat(request.eventId()).isNotBlank();
    assertThat(request.userProfileId()).isEqualTo(loginData.get("user").get("userId").asLong());
    assertThat(request.notificationType()).isEqualTo(NotificationType.TEST_NOTIFICATION);
    assertThat(request.title()).isEqualTo("Landit 알림 테스트");
    assertThat(request.body()).isEqualTo("푸시 알림이 정상적으로 도착했어요.");
    assertThat(request.deepLink()).isEqualTo("/home");
    assertThat(request.occurredAt()).isNotNull();
  }

  /** 인증되지 않은 요청은 dev 테스트 API를 실행할 수 없다. */
  @Test
  void rejectsRequestWithoutAccessToken() throws Exception {
    mockMvc.perform(post(TEST_ENDPOINT)).andExpect(status().isUnauthorized());

    assertThat(pushQueuePublisher.notificationRequest()).isNull();
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

  /** 테스트에서 사용자별 푸시 발행 요청만 기록하는 Queue Publisher를 제공한다. */
  static final class RecordingPushQueuePublisher implements PushQueuePublisher {

    private PushNotificationRequest notificationRequest;

    @Override
    public void publishNotification(PushNotificationRequest request) {
      notificationRequest = request;
    }

    @Override
    public void scheduleReceiptCheck(Long pushDeliveryId, int attempt) {}

    void clear() {
      notificationRequest = null;
    }

    PushNotificationRequest notificationRequest() {
      return notificationRequest;
    }
  }

  /** 테스트용 Push Queue Publisher Bean을 등록한다. */
  @TestConfiguration
  static class TestPushQueuePublisherConfiguration {

    @Bean
    RecordingPushQueuePublisher recordingPushQueuePublisher() {
      return new RecordingPushQueuePublisher();
    }

    @Bean
    PushQueuePublisher pushQueuePublisher(RecordingPushQueuePublisher publisher) {
      return publisher;
    }
  }
}
