// Dev 전용 복습 리마인더 테스트 API의 인증과 Queue 발행 계약을 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import java.time.Instant;
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

/** Dev 전용 복습 리마인더 테스트 API의 인증과 Queue 발행 계약을 검증한다. */
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

  private static final String TEST_ENDPOINT = "/api/v1/internal/test/push/review-reminder";

  @Autowired private MockMvc mockMvc;

  @Autowired private RecordingPushQueuePublisher pushQueuePublisher;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 각 테스트 전에 기록된 배치 발행 시각을 비운다. */
  @BeforeEach
  void clearPublishedReviewReminderBatch() {
    pushQueuePublisher.clear();
  }

  /** 인증된 요청은 현재 시각 기준 복습 리마인더 배치를 Push Queue에 즉시 발행한다. */
  @Test
  void publishesReviewReminderBatchForAuthenticatedRequest() throws Exception {
    Instant requestedAt = Instant.now();

    mockMvc
        .perform(
            post(TEST_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("push-test-api"))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

    assertThat(pushQueuePublisher.reviewReminderBatchOccurredAt()).isAfterOrEqualTo(requestedAt);
  }

  /** 인증되지 않은 요청은 dev 테스트 API를 실행할 수 없다. */
  @Test
  void rejectsRequestWithoutAccessToken() throws Exception {
    mockMvc.perform(post(TEST_ENDPOINT)).andExpect(status().isUnauthorized());

    assertThat(pushQueuePublisher.reviewReminderBatchOccurredAt()).isNull();
  }

  /** 테스트 식별자를 사용하는 가짜 소셜 로그인으로 접근 토큰을 발급한다. */
  private String login(String userKey) throws Exception {
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
    return body.get("data").get("accessToken").asText();
  }

  /** 테스트에서 발행 시각만 기록하는 Push Queue Publisher를 제공한다. */
  static final class RecordingPushQueuePublisher implements PushQueuePublisher {

    private Instant reviewReminderBatchOccurredAt;

    @Override
    public void publishReviewReminderBatch(Instant occurredAt) {
      reviewReminderBatchOccurredAt = occurredAt;
    }

    @Override
    public void scheduleReceiptCheck(Long pushDeliveryId, int attempt) {}

    void clear() {
      reviewReminderBatchOccurredAt = null;
    }

    Instant reviewReminderBatchOccurredAt() {
      return reviewReminderBatchOccurredAt;
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
