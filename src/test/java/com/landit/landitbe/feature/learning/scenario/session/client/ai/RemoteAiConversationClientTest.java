// 원격 AI 대화 클라이언트의 메시지별 피드백 요청 계약을 검증한다.

package com.landit.landitbe.feature.learning.scenario.session.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.content.scenario.question.domain.ResponseDemand;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.scenario.feedback.client.ai.AiSessionFeedbackRequest;
import com.landit.landitbe.feature.learning.scenario.feedback.client.ai.AiSessionFeedbackResult;
import com.landit.landitbe.feature.learning.scenario.feedback.client.ai.AiSessionMessageFeedbackResult;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.FeedbackType;
import com.landit.landitbe.feature.learning.scenario.session.domain.GoalCompletionStatus;
import com.landit.landitbe.feature.learning.scenario.session.innerthought.client.ai.AiInnerThoughtRequest;
import com.landit.landitbe.feature.learning.scenario.session.innerthought.client.ai.AiInnerThoughtResult;
import com.landit.landitbe.feature.learning.scenario.session.message.client.ai.AiNextMessageRequest;
import com.landit.landitbe.feature.learning.scenario.session.message.client.ai.AiNextMessageResult;
import com.landit.landitbe.feature.learning.scenario.session.message.client.ai.AiNextQuestion;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackRequest;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackResult;
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
import org.junit.jupiter.api.DisplayName;
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

  @DisplayName("수준 평가 요청에는 최종 피드백이 소유한 필드를 보내지 않는다.")
  @Test
  void levelAssessmentDoesNotSendFieldsOwnedBySessionFeedback() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/session-level-assessment",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] response =
              "{\"success\":true,\"data\":{\"sessionId\":100,\"levelAssessment\":null}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    AiSessionFeedbackRequest original = aiSessionFeedbackRequest();
    for (List<JsonNode> snapshots :
        java.util.Arrays.<List<JsonNode>>asList(
            null, List.of(jsonMapper.readTree("{\"schemaVersion\":1}")))) {
      remoteClient()
          .generateSessionLevelAssessment(
              new AiSessionFeedbackRequest(
                  original.sessionId(),
                  original.scenario(),
                  original.expectedMessageIds(),
                  original.assessmentMessages(),
                  snapshots));
      JsonNode body = jsonMapper.readTree(requestBody.get());
      assertThat(body.has("completedFeedbacks")).isFalse();
      assertThat(body.get("expectedMessageIds")).hasSize(2);
      assertThat(body.get("assessmentMessages")).hasSize(2);
    }
  }

  @DisplayName("속마음 생성에 대화 문맥을 전송하고 응답을 변환한다.")
  @Test
  void generateInnerThoughtPostsConversationContextAndMapsResponse() throws Exception {
    AtomicReference<String> requestBody = stubInnerThoughtResponse();

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

  @DisplayName("필수 필드가 없는 속마음 생성 응답을 거부한다.")
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

  @DisplayName("속마음 생성의 AI 응답 형식 오류를 그대로 유지한다.")
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

  @DisplayName("속마음 요청의 입출력 오류를 생성 실패로 변환한다.")
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

  @DisplayName("다음 메시지 생성 응답의 수신 확인 정보를 변환한다.")
  @Test
  void generateNextMessageMapsAcknowledgementResponse() throws Exception {
    stubNextMessageAcknowledgement();

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

    assertThat(result.acknowledgement()).isEqualTo("Sounds tasty.");
    assertThat(result.translatedAcknowledgement()).isEqualTo("맛있겠다.");
    assertThat(result.goalCompletionStatus()).isEqualTo(GoalCompletionStatus.PARTIAL);
  }

  @DisplayName("메시지 피드백 계약을 전송하고 준비 중 응답을 변환한다.")
  @Test
  void requestMessageFeedbackPostsContractAndMapsPreparingResponse() throws Exception {
    AtomicReference<String> requestBody = stubMessageFeedbackAccepted(100L, 200L);

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

  @DisplayName("Jackson 3 JsonMapper로 메시지 피드백 요청과 응답을 직렬화 및 역직렬화한다.")
  @Test
  void requestMessageFeedbackSerializesAndDeserializesWithJackson3JsonMapper() throws Exception {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    AtomicReference<String> requestBody =
        stubFeedbackResponse(
            "/api/v1/conversation/message-feedback",
            202,
            """
            {"success":true,"data":{"sessionId":100,"messageId":200,
              "feedbackStatus":"PREPARING","ignoredField":"ignored"},"error":null}
            """);

    AiMessageFeedbackResult result =
        remoteClient(jsonMapper).requestMessageFeedback(aiMessageFeedbackRequest());

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.get("sessionId").asLong()).isEqualTo(100L);
    assertThat(request.get("evaluationContext").get("type").asString()).isEqualTo("AI_MESSAGE");
    assertThat(result)
        .isEqualTo(new AiMessageFeedbackResult(100L, 200L, ProcessingStatus.PREPARING));
  }

  @DisplayName("메시지 피드백 요청에 시나리오 시작 지침 문맥을 전송한다.")
  @Test
  void requestMessageFeedbackPostsScenarioOpeningInstructionContext() throws Exception {
    AtomicReference<String> requestBody = stubMessageFeedbackAccepted(101L, 201L);

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

  @DisplayName("메시지 피드백의 AI 응답 형식 오류를 그대로 유지한다.")
  @Test
  void requestMessageFeedbackPreservesAiResponseInvalidError() {
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> writeErrorResponse(exchange, 502, "AI_RESPONSE_INVALID"));

    RemoteAiConversationClient client = remoteClient();

    assertThatThrownBy(() -> client.requestMessageFeedback(aiMessageFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
  }

  @DisplayName("메시지 피드백의 기타 AI 오류 응답은 생성 실패로 변환한다.")
  @Test
  void requestMessageFeedbackMapsOtherErrorResponseToGenerationFailed() {
    server.createContext(
        "/api/v1/conversation/message-feedback",
        exchange -> writeErrorResponse(exchange, 503, "AI_GENERATION_FAILED"));

    RemoteAiConversationClient client = remoteClient();

    assertThatThrownBy(() -> client.requestMessageFeedback(aiMessageFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @DisplayName("메시지 피드백 요청의 미준비 오류를 생성 실패로 변환한다.")
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

  @DisplayName("최종 피드백 요청에는 세션과 사용자 메시지 ID를 보내고 수준 평가 입력은 제외한다.")
  @Test
  void generateSessionFeedbackPostsContract() throws Exception {
    AtomicReference<String> requestBody = stubSessionFeedbackSuccess();

    remoteClient().generateSessionFeedback(aiSessionFeedbackRequest());

    JsonNode request = jsonMapper.readTree(requestBody.get());
    assertThat(request.get("sessionId").asLong()).isEqualTo(100L);
    assertThat(request.get("scenario").get("serviceAudience").asString())
        .isEqualTo("KOREAN_LEARNER");
    assertThat(request.get("expectedMessageIds"))
        .extracting(JsonNode::asLong)
        .containsExactly(200L, 201L);
    assertThat(request.has("assessmentMessages")).isFalse();
  }

  @DisplayName("최종 피드백 응답의 요약 점수와 수준 평가를 변환한다.")
  @Test
  void generateSessionFeedbackMapsSummaryAndLevelAssessment() {
    stubSessionFeedbackSuccess();

    AiSessionFeedbackResult result =
        remoteClient().generateSessionFeedback(aiSessionFeedbackRequest());

    assertThat(result.sessionId()).isEqualTo(100L);
    assertThat(result.nativeScore()).isEqualTo(75);
    assertThat(result.starRating()).isEqualByComparingTo(new BigDecimal("2.5"));
    assertThat(result.highlightMessage()).isEqualTo("You clearly explained your preference.");
    assertThat(result.summaryMessage()).isEqualTo("Keep connecting your reasons with because.");
    assertThat(result.levelAssessment().core().messages()).hasSize(2);
    assertThat(result.levelAssessment().core().messages().getFirst().domains().grammar().level())
        .isEqualTo(4);
  }

  @DisplayName("최종 피드백 응답의 메시지별 칭찬과 교정 내용을 변환한다.")
  @Test
  void generateSessionFeedbackMapsMessageFeedbacks() {
    stubSessionFeedbackSuccess();

    AiSessionFeedbackResult result =
        remoteClient().generateSessionFeedback(aiSessionFeedbackRequest());

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

  @DisplayName("수준 평가 계약은 최종 피드백 필드를 포함하지 않는다.")
  @Test
  void levelAssessmentDoesNotReceiveFinalFeedbackFields() throws Exception {
    AtomicReference<String> body = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/session-level-assessment",
        exchange -> {
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          writeErrorResponse(exchange, 503, "FEEDBACK_GENERATION_FAILED");
        });
    assertThatThrownBy(
            () -> remoteClient().generateSessionLevelAssessment(aiSessionFeedbackRequest()))
        .isInstanceOf(ApiException.class);
    JsonNode sent = jsonMapper.readTree(body.get());
    assertThat(sent.size()).isEqualTo(4);
    assertThat(sent.has("sessionId")).isTrue();
    assertThat(sent.has("scenario")).isTrue();
    assertThat(sent.has("expectedMessageIds")).isTrue();
    assertThat(sent.has("assessmentMessages")).isTrue();
    assertThat(sent.get("assessmentMessages").get(0).get("responseDemand").asString())
        .isEqualTo("HIGH");
    assertThat(sent.get("assessmentMessages").get(0).get("requiredElements"))
        .extracting(JsonNode::asString)
        .containsExactly("favorite food", "reason");
    assertThat(sent.has("completedFeedbacks")).isFalse();
  }

  @DisplayName("완료된 메시지 피드백을 전달하면서 기존 요청 필드를 유지한다.")
  @Test
  void hotfixForwardsCompletedFeedbacksWithoutChangingLegacyFields() throws Exception {
    AtomicReference<String> body = stubSessionFeedbackSuccess();
    AiSessionFeedbackRequest legacy = aiSessionFeedbackRequest();
    JsonNode snapshot = jsonMapper.readTree("{\"schemaVersion\":1,\"sessionId\":100}");
    remoteClient()
        .generateSessionFeedback(
            new AiSessionFeedbackRequest(
                legacy.sessionId(),
                legacy.scenario(),
                legacy.expectedMessageIds(),
                List.of(),
                List.of(snapshot)));
    JsonNode sent = jsonMapper.readTree(body.get());
    assertThat(sent.get("completedFeedbacks").get(0)).isEqualTo(snapshot);
    assertThat(sent.get("expectedMessageIds")).hasSize(legacy.expectedMessageIds().size());
    assertThat(sent.has("assessmentMessages")).isFalse();
  }

  @DisplayName("최종 피드백 재시도에는 전체 제한 중 남은 대기 시간만 사용한다.")
  @Test
  void hotfixUsesRemainingTimeoutForFinalFeedbackRetry() {
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> {
          try {
            Thread.sleep(200);
            writeSessionFeedbackSuccessResponse(exchange);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    assertThatThrownBy(
            () ->
                remoteClient()
                    .generateSessionFeedback(aiSessionFeedbackRequest(), Duration.ofMillis(30)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(SessionErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  @DisplayName("최종 피드백 요청에는 더 긴 전용 제한 시간을 적용한다.")
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

  @DisplayName("최종 피드백 전용 제한 시간을 초과하면 생성 실패로 변환한다.")
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
                    .isEqualTo(SessionErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  @DisplayName("최종 피드백 요청에서 메시지 피드백 미준비 오류는 피드백 미준비로 변환한다.")
  @Test
  void generateSessionFeedbackMapsMessageFeedbackNotReadyToFeedbackNotReady() {
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> writeErrorResponse(exchange, 409, "MESSAGE_FEEDBACK_NOT_READY"));

    assertThatThrownBy(() -> remoteClient().generateSessionFeedback(aiSessionFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(SessionErrorCode.FEEDBACK_NOT_READY));
  }

  @DisplayName("최종 피드백의 AI 응답 형식 오류를 그대로 유지한다.")
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

  @DisplayName("최종 피드백의 기타 AI 오류 응답은 피드백 생성 실패로 변환한다.")
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
                    .isEqualTo(SessionErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  @DisplayName("최종 피드백 요청의 입출력 오류를 피드백 생성 실패로 변환한다.")
  @Test
  void generateSessionFeedbackMapsIoFailureToFeedbackGenerationFailed() {
    RemoteAiConversationClient client = remoteClient();
    server.stop(0);

    assertThatThrownBy(() -> client.generateSessionFeedback(aiSessionFeedbackRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(SessionErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  private AtomicReference<String> stubMessageFeedbackAccepted(long sessionId, long messageId) {
    return stubFeedbackResponse(
        "/api/v1/conversation/message-feedback",
        202,
        """
        {"success":true,"data":{"sessionId":%d,"messageId":%d,
          "feedbackStatus":"PREPARING"},"error":null}
        """
            .formatted(sessionId, messageId));
  }

  private AtomicReference<String> stubFeedbackResponse(String path, int status, String body) {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        path,
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] response = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    return requestBody;
  }

  private AtomicReference<String> stubSessionFeedbackSuccess() {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/conversation/session-feedback",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          writeSessionFeedbackSuccessResponse(exchange);
        });
    return requestBody;
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
        List.of(200L, 201L),
        List.of(
            new AiSessionFeedbackRequest.AssessmentMessage(
                200L,
                "What food do you like and why?",
                "I like pizza because it is spicy.",
                ResponseDemand.HIGH,
                List.of("favorite food", "reason")),
            new AiSessionFeedbackRequest.AssessmentMessage(
                201L,
                "What did you do yesterday?",
                "I go to the cafe yesterday.",
                ResponseDemand.HIGH,
                List.of("past activity"))));
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
                    Duration.ofSeconds(60),
                    Duration.ofSeconds(20)));
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
            sessionFeedbackRequestTimeout,
            Duration.ofSeconds(20)));
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
                    ],
                    "levelAssessment": {
                      "core": {
                        "messages": [
                          {
                            "messageId": 200,
                            "taskPerformance": "ACHIEVED",
                            "domains": {
                              "situationPerformance": {"level": 4, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "I like pizza"},
                              "grammar": {"level": 4, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "because it is spicy"},
                              "vocabulary": {"level": 4, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "spicy"},
                              "discourse": {"level": 4, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "because"},
                              "interactionPragmatics": {"level": 4, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "I like pizza"}
                            }
                          },
                          {
                            "messageId": 201,
                            "taskPerformance": "PARTIAL",
                            "domains": {
                              "situationPerformance": {"level": 3, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "cafe yesterday"},
                              "grammar": {"level": 2, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "I go"},
                              "vocabulary": {"level": 3, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "cafe"},
                              "discourse": {"level": 3, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "yesterday"},
                              "interactionPragmatics": {"level": 3, "evidenceStatus": "OBSERVED", "evidenceExcerpt": "I go to the cafe"}
                            }
                          }
                        ]
                      },
                      "details": {"strength": "이유를 덧붙였어요.", "improvement": "과거시제를 연습해보세요."}
                    }
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

  private AtomicReference<String> stubInnerThoughtResponse() {
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
    return requestBody;
  }

  private void stubNextMessageAcknowledgement() {
    server.createContext(
        "/api/v1/conversation/next-message",
        exchange -> {
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "acknowledgement": "Sounds tasty.",
                        "translatedAcknowledgement": "맛있겠다.",
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
  }
}
