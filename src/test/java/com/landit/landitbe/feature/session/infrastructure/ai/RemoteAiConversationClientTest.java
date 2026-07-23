// 원격 AI 대화 클라이언트의 메시지별 피드백 요청 계약을 검증한다.

package com.landit.landitbe.feature.session.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.session.application.port.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.application.port.AiInnerThoughtRequest;
import com.landit.landitbe.feature.session.application.port.AiInnerThoughtResult;
import com.landit.landitbe.feature.session.application.port.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.feature.session.application.port.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.feature.session.application.port.AiMessageFeedbackRequest;
import com.landit.landitbe.feature.session.application.port.AiMessageFeedbackResult;
import com.landit.landitbe.feature.session.application.port.AiNextMessageRequest;
import com.landit.landitbe.feature.session.application.port.AiNextMessageResult;
import com.landit.landitbe.feature.session.application.port.AiNextQuestion;
import com.landit.landitbe.feature.session.application.port.AiScenarioContext;
import com.landit.landitbe.feature.session.application.port.AiSessionFeedbackRequest;
import com.landit.landitbe.feature.session.application.port.AiSessionFeedbackResult;
import com.landit.landitbe.feature.session.application.port.AiSessionMessageFeedbackResult;
import com.landit.landitbe.feature.session.domain.FeedbackType;
import com.landit.landitbe.feature.session.domain.GoalCompletionStatus;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 AI 대화 클라이언트의 메시지별 피드백 요청 계약을 검증한다. */
class RemoteAiConversationClientTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private HttpServer server;

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void generateInnerThoughtPostsConversationContextAndMapsResponse() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/inner-thought",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "sessionId": 100,
                        "messageId": 200,
                        "innerThought": "사용자가 이유를 덧붙여 답변했으니 관심을 표현하면 좋겠다.",
                        "innerThoughtType": "GOOD"
                      },
                      "error": null
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    AiInnerThoughtResult result =
        remoteClient()
            .generateInnerThought(
                new AiInnerThoughtRequest(
                    100L,
                    200L,
                    1,
                    new AiScenarioContext(
                        10L,
                        "음식에 대한 대화하기",
                        "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
                        "내 취향과 경험을 영어로 설명해봅니다.",
                        "friend",
                        "KOREAN_LEARNER"),
                    List.of(
                        new AiConversationHistoryMessage(
                            200L,
                            1,
                            "USER",
                            "I like pizza because it is spicy.",
                            "매워서 피자를 좋아해요."))));

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.get("sessionId").asLong()).isEqualTo(100L);
    assertThat(request.get("submittedMessageId").asLong()).isEqualTo(200L);
    assertThat(request.get("submittedTurnNumber").asInt()).isEqualTo(1);
    assertThat(request.get("scenario").get("scenarioId").asLong()).isEqualTo(10L);
    assertThat(request.get("conversationHistory")).hasSize(1);
    assertThat(request.has("nextQuestion")).isFalse();
    assertThat(result)
        .isEqualTo(
            new AiInnerThoughtResult(
                100L, 200L, "사용자가 이유를 덧붙여 답변했으니 관심을 표현하면 좋겠다.", InnerThoughtType.GOOD));
  }

  @Test
  void generateInnerThoughtRejectsResponseMissingRequiredFields() {
    server.createContext(
        "/api/v1/conversation/inner-thought",
        exchange -> {
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "sessionId": 100,
                        "messageId": 200,
                        "innerThoughtType": "GOOD"
                      },
                      "error": null
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    assertThatThrownBy(() -> remoteClient().generateInnerThought(aiInnerThoughtRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
  }

  @Test
  void generateInnerThoughtPreservesAiResponseInvalidError() {
    server.createContext(
        "/api/v1/conversation/inner-thought",
        exchange -> writeErrorResponse(exchange, 502, "AI_RESPONSE_INVALID"));

    assertThatThrownBy(() -> remoteClient().generateInnerThought(aiInnerThoughtRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
  }

  @Test
  void generateInnerThoughtMapsIoFailureToGenerationFailed() {
    RemoteAiConversationClient client = remoteClient();
    server.stop(0);

    assertThatThrownBy(() -> client.generateInnerThought(aiInnerThoughtRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @Test
  void generateNextMessageAcceptsResponseWithoutInnerThought() throws Exception {
    server.createContext(
        "/api/v1/conversation/next-message",
        exchange -> {
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "aiMessage": "What food did you eat recently?",
                        "translatedMessage": "최근에는 어떤 음식을 먹었어?",
                        "goalCompletionStatus": "PARTIAL"
                      },
                      "error": null
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    AiNextMessageResult result =
        remoteClient()
            .generateNextMessage(
                new AiNextMessageRequest(
                    100L,
                    200L,
                    1,
                    new AiScenarioContext(
                        10L,
                        "음식에 대한 대화하기",
                        "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
                        "내 취향과 경험을 영어로 설명해봅니다.",
                        "friend",
                        "KOREAN_LEARNER"),
                    List.of(
                        new AiConversationHistoryMessage(
                            200L, 1, "USER", "I like pizza because it is spicy.", "매워서 피자를 좋아해요.")),
                    new AiNextQuestion(
                        1L, 2, "What food did you eat recently?", "최근에는 어떤 음식을 먹었어?")));

    assertThat(result.aiMessage()).isEqualTo("What food did you eat recently?");
    assertThat(result.translatedMessage()).isEqualTo("최근에는 어떤 음식을 먹었어?");
    assertThat(result.goalCompletionStatus()).isEqualTo(GoalCompletionStatus.PARTIAL);
  }

  @Test
  void requestMessageFeedbackPostsContractAndMapsPreparingResponse() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "sessionId": 100,
                        "messageId": 200,
                        "feedbackStatus": "PREPARING"
                      },
                      "error": null
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(202, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    RemoteAiConversationClient client = remoteClient();

    AiMessageFeedbackResult result =
        client.requestMessageFeedback(
            new AiMessageFeedbackRequest(
                100L,
                200L,
                1,
                2,
                new AiScenarioContext(
                    10L,
                    "음식에 대한 대화하기",
                    "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
                    "내 취향과 경험을 영어로 설명해봅니다.",
                    "friend",
                    "KOREAN_LEARNER"),
                new AiMessageFeedbackEvaluationContext(
                    AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
                    "What food do you like? Why do you like it?",
                    "좋아하는 음식이 있어? 왜 좋아해?"),
                "I like pizza because it is spicy."));

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.get("sessionId").asLong()).isEqualTo(100L);
    assertThat(request.get("messageId").asLong()).isEqualTo(200L);
    assertThat(request.get("turnNumber").asInt()).isEqualTo(1);
    assertThat(request.get("messageSequence").asInt()).isEqualTo(2);
    assertThat(request.get("scenario").get("counterpartRole").asString()).isEqualTo("friend");
    assertThat(request.get("evaluationContext").get("type").asString()).isEqualTo("AI_MESSAGE");
    assertThat(request.get("evaluationContext").get("content").asString())
        .isEqualTo("What food do you like? Why do you like it?");
    assertThat(request.get("evaluationContext").get("translatedContent").asString())
        .isEqualTo("좋아하는 음식이 있어? 왜 좋아해?");
    assertThat(request.get("userMessage").asString())
        .isEqualTo("I like pizza because it is spicy.");
    assertThat(result)
        .isEqualTo(new AiMessageFeedbackResult(100L, 200L, ProcessingStatus.PREPARING));
  }

  @Test
  void requestMessageFeedbackSerializesAndDeserializesWithJackson3JsonMapper() throws Exception {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "sessionId": 100,
                        "messageId": 200,
                        "feedbackStatus": "PREPARING",
                        "ignoredField": "ignored"
                      },
                      "error": null
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(202, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    AiMessageFeedbackResult result =
        remoteClient(jsonMapper).requestMessageFeedback(aiMessageFeedbackRequest());

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.get("sessionId").asLong()).isEqualTo(100L);
    assertThat(request.get("evaluationContext").get("type").asString()).isEqualTo("AI_MESSAGE");
    assertThat(result)
        .isEqualTo(new AiMessageFeedbackResult(100L, 200L, ProcessingStatus.PREPARING));
  }

  @Test
  void requestMessageFeedbackPostsScenarioOpeningInstructionContext() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "sessionId": 101,
                        "messageId": 201,
                        "feedbackStatus": "PREPARING"
                      },
                      "error": null
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(202, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    RemoteAiConversationClient client = remoteClient();

    client.requestMessageFeedback(
        new AiMessageFeedbackRequest(
            101L,
            201L,
            1,
            1,
            new AiScenarioContext(
                20L,
                "카페에서 음료 주문하기",
                "카페 점원에게 원하는 음료를 주문합니다.",
                "원하는 음료를 자연스럽고 공손하게 주문합니다.",
                "cafe staff",
                "KOREAN_LEARNER"),
            new AiMessageFeedbackEvaluationContext(
                AiMessageFeedbackEvaluationContextType.SCENARIO_OPENING_INSTRUCTION,
                "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                null),
            "Can I get an iced americano?"));

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.get("messageSequence").asInt()).isEqualTo(1);
    assertThat(request.get("evaluationContext").get("type").asString())
        .isEqualTo("SCENARIO_OPENING_INSTRUCTION");
    assertThat(request.get("evaluationContext").get("content").asString())
        .isEqualTo("점원에게 먼저 주문하고 싶은 음료를 말해보세요.");
    assertThat(request.get("evaluationContext").get("translatedContent").isNull()).isTrue();
    assertThat(request.get("userMessage").asString()).isEqualTo("Can I get an iced americano?");
  }

  @Test
  void requestMessageFeedbackPreservesAiResponseInvalidError() {
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> {
          byte[] responseBody =
              """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": "AI_RESPONSE_INVALID",
                        "message": "AI 응답 형식이 올바르지 않습니다."
                      }
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(502, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    RemoteAiConversationClient client = remoteClient();

    assertThatThrownBy(() -> client.requestMessageFeedback(aiMessageFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
  }

  @Test
  void requestMessageFeedbackMapsOtherErrorResponseToGenerationFailed() {
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> {
          byte[] responseBody =
              """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": "AI_GENERATION_FAILED",
                        "message": "AI 응답 생성에 실패했습니다."
                      }
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(503, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    RemoteAiConversationClient client = remoteClient();

    assertThatThrownBy(() -> client.requestMessageFeedback(aiMessageFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @Test
  void requestMessageFeedbackMapsMessageFeedbackNotReadyToGenerationFailed() {
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> writeErrorResponse(exchange, 409, "MESSAGE_FEEDBACK_NOT_READY"));

    assertThatThrownBy(() -> remoteClient().requestMessageFeedback(aiMessageFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @Test
  void generateSessionFeedbackPostsContractAndMapsResponse() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          writeSessionFeedbackSuccessResponse(exchange);
        });

    RemoteAiConversationClient client = remoteClient();

    AiSessionFeedbackResult result = client.generateSessionFeedback(aiSessionFeedbackRequest());

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.get("sessionId").asLong()).isEqualTo(100L);
    assertThat(request.get("scenario").get("serviceAudience").asString())
        .isEqualTo("KOREAN_LEARNER");
    assertThat(request.get("expectedMessageIds"))
        .extracting(JsonNode::asLong)
        .containsExactly(200L, 201L);
    assertThat(result.sessionId()).isEqualTo(100L);
    assertThat(result.nativeScore()).isEqualTo(75);
    assertThat(result.starRating()).isEqualByComparingTo(new BigDecimal("2.5"));
    assertThat(result.highlightMessage()).isEqualTo("You clearly explained your preference.");
    assertThat(result.summaryMessage()).isEqualTo("Keep connecting your reasons with because.");
    assertThat(result.messageFeedbacks())
        .containsExactly(
            new AiSessionMessageFeedbackResult(
                200L,
                FeedbackType.GOOD,
                "한국어로 자연스럽게 이유를 덧붙인 표현과 비슷해요.",
                null,
                "The reason makes your preference easy to understand.",
                null,
                null,
                "I like pizza because it is spicy."),
            new AiSessionMessageFeedbackResult(
                201L,
                FeedbackType.NEEDS_IMPROVEMENT,
                "한국어에서도 시제를 맞춰 말하는 것과 같아요.",
                "Your main idea is clear.",
                null,
                "I went to the cafe yesterday.",
                "Use the past tense for a completed action.",
                "I went to the cafe yesterday."));
  }

  @Test
  void generateSessionFeedbackUsesLongerDedicatedRequestTimeout() {
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> {
          try {
            Thread.sleep(70L);
            writeSessionFeedbackSuccessResponse(exchange);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            exchange.close();
          }
        });

    AiSessionFeedbackResult result =
        remoteClient(Duration.ofMillis(50), Duration.ofMillis(120))
            .generateSessionFeedback(aiSessionFeedbackRequest());

    assertThat(result.sessionId()).isEqualTo(100L);
  }

  @Test
  void generateSessionFeedbackMapsDedicatedRequestTimeoutToFeedbackGenerationFailed() {
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> {
          try {
            Thread.sleep(200L);
            writeSessionFeedbackSuccessResponse(exchange);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            exchange.close();
          }
        });

    assertThatThrownBy(
            () ->
                remoteClient(Duration.ofSeconds(60), Duration.ofMillis(50))
                    .generateSessionFeedback(aiSessionFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  @Test
  void generateSessionFeedbackMapsMessageFeedbackNotReadyToFeedbackNotReady() {
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> writeErrorResponse(exchange, 409, "MESSAGE_FEEDBACK_NOT_READY"));

    assertThatThrownBy(() -> remoteClient().generateSessionFeedback(aiSessionFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_NOT_READY));
  }

  @Test
  void generateSessionFeedbackPreservesAiResponseInvalidError() {
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> writeErrorResponse(exchange, 502, "AI_RESPONSE_INVALID"));

    assertThatThrownBy(() -> remoteClient().generateSessionFeedback(aiSessionFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
  }

  @Test
  void generateSessionFeedbackMapsOtherErrorResponseToFeedbackGenerationFailed() {
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> writeErrorResponse(exchange, 503, "AI_GENERATION_FAILED"));

    assertThatThrownBy(() -> remoteClient().generateSessionFeedback(aiSessionFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  @Test
  void generateSessionFeedbackMapsIoFailureToFeedbackGenerationFailed() {
    RemoteAiConversationClient client = remoteClient();
    server.stop(0);

    assertThatThrownBy(() -> client.generateSessionFeedback(aiSessionFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  private AiMessageFeedbackRequest aiMessageFeedbackRequest() {
    return new AiMessageFeedbackRequest(
        100L,
        200L,
        1,
        2,
        new AiScenarioContext(
            10L,
            "음식에 대한 대화하기",
            "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
            "내 취향과 경험을 영어로 설명해봅니다.",
            "friend",
            "KOREAN_LEARNER"),
        new AiMessageFeedbackEvaluationContext(
            AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
            "What food do you like?",
            "어떤 음식을 좋아해?"),
        "I like pizza.");
  }

  private AiInnerThoughtRequest aiInnerThoughtRequest() {
    return new AiInnerThoughtRequest(
        100L,
        200L,
        1,
        new AiScenarioContext(
            10L,
            "음식에 대한 대화하기",
            "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
            "내 취향과 경험을 영어로 설명해봅니다.",
            "friend",
            "KOREAN_LEARNER"),
        List.of(
            new AiConversationHistoryMessage(
                200L, 1, "USER", "I like pizza because it is spicy.", "매워서 피자를 좋아해요.")));
  }

  private AiSessionFeedbackRequest aiSessionFeedbackRequest() {
    return new AiSessionFeedbackRequest(
        100L,
        new AiScenarioContext(
            10L,
            "음식에 대한 대화하기",
            "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
            "내 취향과 경험을 영어로 설명해봅니다.",
            "friend",
            "KOREAN_LEARNER"),
        List.of(200L, 201L));
  }

  private RemoteAiConversationClient remoteClient() {
    return remoteClient(Duration.ofSeconds(60));
  }

  private RemoteAiConversationClient remoteClient(JsonMapper jsonMapper) throws Exception {
    return (RemoteAiConversationClient)
        RemoteAiConversationClient.class
            .getConstructor(JsonMapper.class, AiClientProperties.class)
            .newInstance(
                jsonMapper,
                new AiClientProperties(
                    baseUrl(),
                    "remote",
                    "KOREAN_LEARNER",
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(60)));
  }

  private RemoteAiConversationClient remoteClient(Duration requestTimeout) {
    return remoteClient(requestTimeout, requestTimeout);
  }

  private RemoteAiConversationClient remoteClient(
      Duration requestTimeout, Duration sessionFeedbackRequestTimeout) {
    return new RemoteAiConversationClient(
        jsonMapper,
        new AiClientProperties(
            baseUrl(),
            "remote",
            "KOREAN_LEARNER",
            Duration.ofSeconds(5),
            requestTimeout,
            sessionFeedbackRequestTimeout));
  }

  private void writeSessionFeedbackSuccessResponse(com.sun.net.httpserver.HttpExchange exchange)
      throws java.io.IOException {
    byte[] responseBody =
        """
                {
                  "success": true,
                  "data": {
                    "sessionId": 100,
                    "nativeScore": 75,
                    "starRating": 2.5,
                    "highlightMessage": "You clearly explained your preference.",
                    "summaryMessage": "Keep connecting your reasons with because.",
                    "messageFeedbacks": [
                      {
                        "messageId": 200,
                        "feedbackType": "GOOD",
                        "baseLocaleAnalogy": "한국어로 자연스럽게 이유를 덧붙인 표현과 비슷해요.",
                        "positiveFeedback": null,
                        "feedbackDetail": "The reason makes your preference easy to understand.",
                        "correctionExpression": null,
                        "correctionReason": null,
                        "benchmarkMessage": "I like pizza because it is spicy."
                      },
                      {
                        "messageId": 201,
                        "feedbackType": "NEEDS_IMPROVEMENT",
                        "baseLocaleAnalogy": "한국어에서도 시제를 맞춰 말하는 것과 같아요.",
                        "positiveFeedback": "Your main idea is clear.",
                        "feedbackDetail": null,
                        "correctionExpression": "I went to the cafe yesterday.",
                        "correctionReason": "Use the past tense for a completed action.",
                        "benchmarkMessage": "I went to the cafe yesterday."
                      }
                    ]
                  },
                  "error": null
                }
        """
            .getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  private void writeErrorResponse(
      com.sun.net.httpserver.HttpExchange exchange, int status, String code)
      throws java.io.IOException {
    byte[] responseBody =
        """
        {
          "success": false,
          "data": null,
          "error": {
            "code": "%s",
            "message": "AI 요청에 실패했습니다."
          }
        }
        """
            .formatted(code)
            .getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  private String baseUrl() {
    return "http://localhost:%d/".formatted(server.getAddress().getPort());
  }
}
