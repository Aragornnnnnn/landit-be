// 원격 프리톡 AI 클라이언트의 HTTP 계약과 오류 변환을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.domain.CharacterEmotion;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExistingExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsResult;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingReason;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 프리톡 AI 클라이언트의 HTTP 계약과 오류 변환을 검증한다. */
class RemoteAiFreeTalkClientTest {

  private static final String EXPRESSION_TEXT = "I'm up for that";
  private static final String EXPRESSION_MEANING = "좋아, 그거 하자";
  private static final String EXPRESSION_USAGE = "제안에 동의할 때 사용";

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

  @DisplayName("프리톡 시작 요청의 주제와 이름 제외 계약을 전송하고 감정을 변환한다.")
  @Test
  void postsOpeningContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        requests,
        """
            {
              "success": true,
              "data": {
                "aiMessage": "How was your weekend?",
                "translatedMessage": "주말 어땠어?",
                "emotion": "HAPPY"
              },
              "error": null
            }
        """);

    AiFreeTalkOpeningResult opening = remoteClient().generateOpening(openingRequest());

    assertThat(requests.get("/api/v1/free-talk/opening").get("topic").get("topicId").asLong())
        .isEqualTo(2L);
    assertThat(requests.get("/api/v1/free-talk/opening").has("partnerDisplayName")).isFalse();
    assertThat(opening.emotion()).isEqualTo(CharacterEmotion.HAPPY);
  }

  @DisplayName("프리톡 발화 요청은 응답 모드를 포함하고 파트너 이름을 제외한다.")
  @Test
  void postsTurnContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/turn",
        requests,
        """
            {
              "success": true,
              "data": {
                "userExitIntentDetected": false,
                "inferredTitle": "주말 이야기",
                "aiMessage": "That sounds fun.",
                "translatedMessage": "재밌겠다.",
                "emotion": "HAPPY",
                "innerThought": "즐거웠나 보다.",
                "innerThoughtType": "GOOD"
              },
              "error": null
            }
        """);

    remoteClient().generateTurn(turnRequest());

    assertThat(requests.get("/api/v1/free-talk/turn").get("responseMode").asString())
        .isEqualTo("NORMAL");
    assertThat(requests.get("/api/v1/free-talk/turn").has("partnerDisplayName")).isFalse();
  }

  @DisplayName("프리톡 속마음 응답의 평가 유형을 변환한다.")
  @Test
  void postsInnerThoughtContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        requests,
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD"},"error":null}
        """);

    AiFreeTalkInnerThoughtResult innerThought =
        remoteClient().generateInnerThought(innerThoughtRequest());

    assertThat(innerThought.innerThoughtType().name()).isEqualTo("GOOD");
  }

  @DisplayName("프리톡 종료 요청에 종료 사유와 제목 생성 조건을 보내고 응답을 변환한다.")
  @Test
  void postsClosingContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/closing",
        requests,
        """
            {
              "success": true,
              "data": {
                "inferredTitle": "Weekend Hiking",
                "aiMessage": "It was nice talking with you.",
                "translatedMessage": "이야기해서 좋았어.",
                "emotion": "NEUTRAL",
                "innerThought": "대화를 잘 마무리했다.",
                "innerThoughtType": "NORMAL"
              },
              "error": null
            }
        """);

    AiFreeTalkClosingResult closing = remoteClient().generateClosing(closingRequest());

    assertThat(requests.get("/api/v1/free-talk/closing").has("partnerDisplayName")).isFalse();
    assertThat(requests.get("/api/v1/free-talk/closing").get("closingReason").asString())
        .isEqualTo("USER_CONFIRMED");
    assertThat(requests.get("/api/v1/free-talk/closing").get("titleGenerationRequired").asBoolean())
        .isTrue();
    assertThat(closing.inferredTitle()).isEqualTo("Weekend Hiking");
    assertThat(closing.translatedMessage()).isEqualTo("이야기해서 좋았어.");
  }

  @DisplayName("기존 표현 추천 계약을 전송하고 성공 응답을 변환한다.")
  @Test
  void postsExistingExpressionContractAndMapsSuccessfulResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/expression-recommendations",
        requests,
        """
            {
              "success": true,
              "data": {
                "recommendations": [{
                  "displayOrder": 1,
                  "existingExpressionId": 7
                }]
              },
              "error": null
            }
        """);
    RemoteAiFreeTalkClient client = remoteClient();
    AiFreeTalkExpressionRecommendationsResult recommendations =
        client.recommendExpressions(recommendationsRequest());

    assertThat(
            requests
                .get("/api/v1/free-talk/expression-recommendations")
                .get("existingExpressions")
                .get(0)
                .get("expressionId")
                .asLong())
        .isEqualTo(7L);
    assertThat(recommendations.recommendations()).hasSize(1);
  }

  @DisplayName("일반 첫 대화 요청에는 기억 조회보다 긴 대기 시간을 적용한다.")
  @Test
  void normalOpeningWaitsBeyondMemoryTimeout() {
    registerDelayedResponse(
        "/api/v1/free-talk/opening",
        2_300,
        successResponse("{\"aiMessage\":\"Hello!\",\"translatedMessage\":\"안녕!\"}"));
    assertThat(remoteClient(Duration.ofSeconds(5)).generateOpening(openingRequest()).aiMessage())
        .isEqualTo("Hello!");
  }

  private void registerDelayedResponse(String path, long delayMillis, String response) {
    server.createContext(
        path,
        exchange -> {
          try {
            Thread.sleep(delayMillis);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
  }

  @DisplayName("첫 대화 요청에 기억 문맥을 보내고 사용된 기억 ID를 응답에서 읽는다.")
  @Test
  void mapsUsedMemoryIdsAndSendsMemoryContextForOpening() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        requests,
        successResponse(
            "{\"aiMessage\":\"How is the interview going?\","
                + "\"translatedMessage\":\"면접은 잘 되어가?\","
                + "\"usedMemoryIds\":[77]}"));

    AiFreeTalkOpeningResult result =
        remoteClient()
            .generateOpening(
                new AiFreeTalkOpeningRequest(
                    300L,
                    "chloe",
                    "EN",
                    "KR",
                    new AiFreeTalkTopic(2L, "주말 계획", "Ask about weekend plans."),
                    List.of(
                        new AiFreeTalkMemoryContext(
                            77L,
                            ConversationMemoryType.EVENT,
                            "면접 계획",
                            LocalDateTime.of(2026, 7, 1, 9, 0),
                            LocalDateTime.of(2026, 7, 31, 23, 59),
                            LocalDateTime.of(2026, 8, 1, 10, 30)))));

    assertThat(
            requests
                .get("/api/v1/free-talk/opening")
                .get("memoryContext")
                .get(0)
                .get("memoryId")
                .asLong())
        .isEqualTo(77L);
    JsonNode memoryContext = requests.get("/api/v1/free-talk/opening").get("memoryContext").get(0);
    assertThat(memoryContext.get("validFrom").asText()).isEqualTo("2026-07-01T09:00:00");
    assertThat(memoryContext.get("validTo").asText()).isEqualTo("2026-07-31T23:59:00");
    assertThat(memoryContext.get("observedAt").asText()).isEqualTo("2026-08-01T10:30:00");
    assertThat(result.usedMemoryIds()).containsExactly(77L);
  }

  @DisplayName("문맥에 없는 기억 ID는 정리하되 대화 응답 자체는 거부하지 않는다.")
  @Test
  void normalizesUsedMemoryIdOutsideContextWithoutRejectingConversation() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        new ConcurrentHashMap<>(),
        successResponse(
            "{\"aiMessage\":\"How is the interview going?\","
                + "\"translatedMessage\":\"면접은 잘 되어가?\","
                + "\"usedMemoryIds\":[88]}"));

    AiFreeTalkOpeningResult result =
        remoteClient()
            .generateOpening(
                new AiFreeTalkOpeningRequest(
                    300L,
                    "chloe",
                    "EN",
                    "KR",
                    new AiFreeTalkTopic(2L, "주말 계획", "Ask about weekend plans."),
                    List.of(
                        new AiFreeTalkMemoryContext(77L, ConversationMemoryType.EVENT, "면접 계획"))));

    assertThat(result.usedMemoryIds()).isEmpty();
  }

  @DisplayName("필수 필드가 빠진 프리톡 AI 응답을 거부한다.")
  @Test
  void rejectsResponsesMissingRequiredFields() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        new ConcurrentHashMap<>(),
        """
            {"success":true,"data":{"translatedMessage":"주말 어땠어?","emotion":"HAPPY"},"error":null}
        """);

    assertThatThrownBy(() -> remoteClient().generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
  }

  @DisplayName("프리톡 시작 응답은 감정 값이 null이어도 허용한다.")
  @Test
  void acceptsNullEmotionForOpening() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "aiMessage": "How was your weekend?",
                "translatedMessage": "주말 어땠어?",
                "emotion": null
              },
              "error": null
            }
        """);

    assertThat(remoteClient().generateOpening(openingRequest()).emotion()).isNull();
  }

  @DisplayName("프리톡 발화 응답은 감정 값이 null이어도 허용한다.")
  @Test
  void acceptsNullEmotionForTurn() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/turn",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "userExitIntentDetected": false,
                "inferredTitle": "주말 이야기",
                "aiMessage": "That sounds fun.",
                "translatedMessage": "재밌겠다.",
                "emotion": null
              },
              "error": null
            }
        """);

    assertThat(remoteClient().generateTurn(turnRequest()).emotion()).isNull();
  }

  @DisplayName("프리톡 종료 응답은 감정 값이 null이어도 허용한다.")
  @Test
  void acceptsNullEmotionForClosing() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/closing",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "aiMessage": "It was nice talking with you.",
                "translatedMessage": "이야기해서 좋았어.",
                "emotion": null
              },
              "error": null
            }
        """);

    assertThat(remoteClient().generateClosing(closingRequest()).emotion()).isNull();
  }

  @DisplayName("빈 종료 제목은 없는 것으로 처리하고 종료 메시지는 보존한다.")
  @Test
  void treatsBlankClosingTitleAsMissingWhilePreservingClosingMessage() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/closing",
        200,
        successResponse(
            "{\"inferredTitle\":\"   \","
                + "\"aiMessage\":\"It was nice talking with you.\","
                + "\"translatedMessage\":\"이야기해서 좋았어.\",\"emotion\":null}"));

    AiFreeTalkClosingResult result = remoteClient().generateClosing(closingRequest());

    assertThat(result.inferredTitle()).isNull();
    assertThat(result.aiMessage()).isEqualTo("It was nice talking with you.");
    assertThat(result.translatedMessage()).isEqualTo("이야기해서 좋았어.");
  }

  @DisplayName("AI의 502 응답 형식 오류를 유지하고 503은 생성 실패로 변환한다.")
  @Test
  void preservesResponseInvalidForUpstream502AndMaps503ToGenerationFailure() throws Exception {
    server.createContext(
        "/api/v1/free-talk/opening",
        exchange -> writeErrorResponse(exchange, 502, "AI_RESPONSE_INVALID"));

    assertThatThrownBy(() -> remoteClient().generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));

    server.removeContext("/api/v1/free-talk/opening");
    server.createContext(
        "/api/v1/free-talk/opening",
        exchange -> writeErrorResponse(exchange, 503, "AI_RESPONSE_INVALID"));

    assertThatThrownBy(() -> remoteClient().generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @DisplayName("프리톡 AI 네트워크 오류를 생성 실패로 변환한다.")
  @Test
  void mapsNetworkFailureToGenerationFailure() {
    RemoteAiFreeTalkClient client = remoteClient();
    server.stop(0);

    assertThatThrownBy(() -> client.generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @DisplayName("성공 응답의 data가 null이면 프리톡 AI 응답을 거부한다.")
  @Test
  void rejectsNullDataOnSuccessfulResponse() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/opening", 200, "{\"success\":true,\"data\":null,\"error\":null}");

    assertGenerationError(
        () -> remoteClient().generateOpening(openingRequest()), ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("발화와 종료 및 표현 추천의 잘못된 AI 응답을 거부한다.")
  @Test
  void rejectsRepresentativeInvalidResponsesForTurnClosingAndRecommendations() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/turn",
        200,
        successResponse(
            String.join(
                "",
                "{\"userExitIntentDetected\":true,\"inferredTitle\":null,",
                "\"aiMessage\":\"See you.\",\"translatedMessage\":null,\"emotion\":null,",
                "\"innerThought\":null,\"innerThoughtType\":null}")));
    assertGenerationError(
        () -> remoteClient().generateTurn(turnRequest()), ErrorCode.AI_RESPONSE_INVALID);

    registerRawResponse(
        "/api/v1/free-talk/closing",
        200,
        successResponse(
            String.join(
                "",
                "{\"aiMessage\":null,\"translatedMessage\":\"또 봐.\",",
                "\"emotion\":null,\"innerThought\":\"잘 마무리했다.\",",
                "\"innerThoughtType\":\"GOOD\"}")));
    assertGenerationError(
        () -> remoteClient().generateClosing(closingRequest()), ErrorCode.AI_RESPONSE_INVALID);

    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse("{\"recommendations\":[{\"displayOrder\":1}]}"));
    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("후보에 없는 기존 표현 ID를 추천한 AI 응답을 거부한다.")
  @Test
  void rejectsRecommendationWithUnknownExistingExpressionId() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse(
            recommendationsData(8L, "I'm up for that", "좋아, 그거 하자", "제안에 동의할 때 사용", 1)));

    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("기존 표현 ID가 빠진 추천 응답을 거부한다.")
  @Test
  void rejectsRecommendationMissingExistingExpressionId() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse("{\"recommendations\":[{\"displayOrder\":1}]}"));

    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("AI 서버가 검증한 기존 표현 추천 메타데이터를 허용한다.")
  @Test
  void acceptsExistingRecommendationMetadataValidatedByAiServer() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse(
            recommendationsData(7L, "I am up for that", "좋아, 그거 하자", "제안에 동의할 때 사용", 1)));

    AiFreeTalkExpressionRecommendationsResult result =
        remoteClient().recommendExpressions(recommendationsRequest());

    assertThat(result.recommendations().getFirst().existingExpressionId()).isEqualTo(7L);
  }

  @DisplayName("노출 순서가 중복된 표현 추천을 거부한다.")
  @Test
  void rejectsRecommendationWithDuplicateDisplayOrder() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse(
            "{\"recommendations\":["
                + recommendationData(7L, EXPRESSION_TEXT, EXPRESSION_MEANING, EXPRESSION_USAGE, 1)
                + ","
                + recommendationData(7L, EXPRESSION_TEXT, EXPRESSION_MEANING, EXPRESSION_USAGE, 1)
                + "]}"));

    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("프리톡 AI 요청의 시간 초과와 인터럽트를 생성 실패로 변환한다.")
  @Test
  void mapsTimeoutAndInterruptedRequestToGenerationFailure() throws Exception {
    server.createContext(
        "/api/v1/free-talk/opening",
        exchange -> {
          try {
            Thread.sleep(200);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          }
          exchange.close();
        });

    assertGenerationError(
        () -> remoteClient(Duration.ofMillis(50)).generateOpening(openingRequest()),
        ErrorCode.AI_GENERATION_FAILED);

    Thread.currentThread().interrupt();
    try {
      assertGenerationError(
          () -> remoteClient().generateOpening(openingRequest()), ErrorCode.AI_GENERATION_FAILED);
    } finally {
      Thread.interrupted();
    }
  }

  private void assertGenerationError(Runnable invocation, ErrorCode expectedErrorCode) {
    assertThatThrownBy(invocation::run)
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
  }

  private void registerJsonResponse(String path, Map<String, JsonNode> requests, String response)
      throws Exception {
    server.createContext(
        path,
        exchange -> {
          requests.put(
              path,
              jsonMapper.readTree(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
          byte[] responseBody = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
  }

  private void registerRawResponse(String path, int status, String response) throws Exception {
    server.createContext(
        path,
        exchange -> {
          byte[] responseBody = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
  }

  private void writeErrorResponse(
      com.sun.net.httpserver.HttpExchange exchange, int status, String code)
      throws java.io.IOException {
    byte[] responseBody =
        ("{\"success\":false,\"data\":null,\"error\":{\"code\":\"" + code + "\"}}")
            .getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  private RemoteAiFreeTalkClient remoteClient() {
    return remoteClient(Duration.ofSeconds(1));
  }

  private RemoteAiFreeTalkClient remoteClient(Duration requestTimeout) {
    return new RemoteAiFreeTalkClient(
        jsonMapper,
        new AiClientProperties(
            "http://localhost:" + server.getAddress().getPort(),
            "remote",
            "KOREAN_LEARNER",
            Duration.ofSeconds(1),
            requestTimeout,
            Duration.ofSeconds(1),
            Duration.ofSeconds(20)));
  }

  private String successResponse(String data) {
    return "{\"success\":true,\"data\":" + data + ",\"error\":null}";
  }

  private String recommendationsData(
      long expressionId,
      String targetExpressionText,
      String meaning,
      String usageSummary,
      int displayOrder) {
    return "{\"recommendations\":["
        + recommendationData(
            expressionId, targetExpressionText, meaning, usageSummary, displayOrder)
        + "]}";
  }

  private String recommendationData(
      long expressionId,
      String targetExpressionText,
      String meaning,
      String usageSummary,
      int displayOrder) {
    return String.join(
        "\n",
        "{",
        "  \"displayOrder\": %d,".formatted(displayOrder),
        "  \"sourceType\": \"EXISTING\",",
        "  \"existingExpressionId\": %d,".formatted(expressionId),
        "  \"targetExpressionText\": \"%s\",".formatted(targetExpressionText),
        "  \"baseExpressionMeaningText\": \"%s\",".formatted(meaning),
        "  \"usageSummary\": \"%s\"".formatted(usageSummary),
        "}");
  }

  private AiFreeTalkOpeningRequest openingRequest() {
    return new AiFreeTalkOpeningRequest(
        300L,
        "chloe",
        "EN",
        "KR",
        new AiFreeTalkTopic(2L, "주말 계획", "Ask about the user's weekend plans."),
        List.of());
  }

  private AiFreeTalkTurnRequest turnRequest() {
    return new AiFreeTalkTurnRequest(
        300L,
        "chloe",
        3002L,
        1,
        "EN",
        "KR",
        AiFreeTalkResponseMode.NORMAL,
        true,
        null,
        history(),
        List.of());
  }

  private AiFreeTalkClosingRequest closingRequest() {
    return new AiFreeTalkClosingRequest(
        300L,
        "chloe",
        3002L,
        1,
        "EN",
        "KR",
        AiFreeTalkClosingReason.USER_CONFIRMED,
        true,
        new AiFreeTalkTopic(null, "주말 이야기", null),
        history());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest() {
    return new AiFreeTalkInnerThoughtRequest(300L, "chloe", 3002L, 1, "EN", "KR", null, history());
  }

  private AiFreeTalkExpressionRecommendationsRequest recommendationsRequest() {
    return new AiFreeTalkExpressionRecommendationsRequest(
        300L,
        "EN",
        "KR",
        history(),
        List.of(
            new AiFreeTalkExistingExpression(
                7L, EXPRESSION_TEXT, EXPRESSION_MEANING, EXPRESSION_USAGE)));
  }

  private List<AiConversationHistoryMessage> history() {
    return List.of(
        new AiConversationHistoryMessage(3002L, 1, "USER", "I'm going hiking with friends.", null));
  }
}
