// 실제 구·신 AI HTTP 서버가 hotfix의 요청과 응답 계약을 수용하는지 검증한다.

package com.landit.landitbe.feature.session.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "LAN474_AI_URL", matches = ".+")
class AiDeploymentCompatibilityIntegrationTest {
  @Test
  void actualAiAcceptsTheHotfixContractAndCacheRecovery() throws Exception {
    String baseUrl = System.getenv("LAN474_AI_URL");
    assertThat(URI.create(baseUrl).getHost()).isEqualTo("127.0.0.1");
    Duration timeout = Duration.ofSeconds(10);
    var client =
        new RemoteAiConversationClient(
            new JsonMapper(),
            new AiClientProperties(
                baseUrl, "remote", "KOREAN_LEARNER", timeout, timeout, timeout, timeout));
    var scenario =
        new AiScenarioContext(10L, "음식 이야기", "좋아하는 음식 이야기", "취향을 설명한다", "friend", "KOREAN_LEARNER");
    var message =
        new AiMessageFeedbackRequest(
            100L,
            200L,
            1,
            2,
            scenario,
            new AiMessageFeedbackEvaluationContext(
                AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
                "What food do you like, and why?",
                "어떤 음식을 좋아하고, 왜 좋아해?"),
            "I like pizza because it is tasty.");
    var completed = client.requestMessageFeedback(message);
    assertThat(completed.feedbackStatus()).isEqualTo(ProcessingStatus.PREPARING);
    boolean supportsSnapshot = Boolean.parseBoolean(System.getenv("LAN474_EXPECT_SNAPSHOT"));
    assertThat(completed.completedFeedback() != null).isEqualTo(supportsSnapshot);
    var request =
        new AiSessionFeedbackRequest(
            100L,
            scenario,
            List.of(200L),
            supportsSnapshot ? List.of(completed.completedFeedback()) : null);
    if (supportsSnapshot) {
      clearCache(baseUrl);
    }
    assertThat(client.generateSessionFeedback(request).messageFeedbacks()).hasSize(1);
    clearCache(baseUrl);
    if (supportsSnapshot) {
      assertThat(client.generateSessionFeedback(request).messageFeedbacks()).hasSize(1);
    } else {
      assertThatThrownBy(() -> client.generateSessionFeedback(request))
          .isInstanceOfSatisfying(
              ApiException.class,
              error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_NOT_READY));
      client.requestMessageFeedback(message);
      assertThat(client.generateSessionFeedback(request).messageFeedbacks()).hasSize(1);
    }
  }

  private void clearCache(String baseUrl) throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create(baseUrl + "/__test__/clear-cache"))
                  .timeout(Duration.ofSeconds(5))
                  .POST(HttpRequest.BodyPublishers.noBody())
                  .build(),
              HttpResponse.BodyHandlers.discarding());
      assertThat(response.statusCode()).isEqualTo(200);
    }
  }
}
