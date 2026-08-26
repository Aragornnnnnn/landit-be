// 원격 프리톡 AI 클라이언트의 HTTP 계약과 오류 변환을 검증한다.

package com.landit.landitbe.feature.session.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

  @Test
  void postsEachFreeTalkContractAndMapsSuccessfulResponses() throws Exception {
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
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        requests,
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD"},"error":null}
        """);
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
    RemoteAiFreeTalkClient client = remoteClient();

    AiFreeTalkOpeningResult opening = client.generateOpening(openingRequest());
    AiFreeTalkTurnResult turn = client.generateTurn(turnRequest());
    AiFreeTalkInnerThoughtResult innerThought = client.generateInnerThought(innerThoughtRequest());
    AiFreeTalkClosingResult closing = client.generateClosing(closingRequest());

    assertThat(requests.get("/api/v1/free-talk/opening").get("topic").get("topicId").asLong())
        .isEqualTo(2L);
    assertThat(requests.get("/api/v1/free-talk/turn").get("responseMode").asString())
        .isEqualTo("NORMAL");
    assertThat(requests.get("/api/v1/free-talk/opening").has("partnerDisplayName")).isFalse();
    assertThat(requests.get("/api/v1/free-talk/turn").has("partnerDisplayName")).isFalse();
    assertThat(requests.get("/api/v1/free-talk/closing").has("partnerDisplayName")).isFalse();
    assertThat(requests.get("/api/v1/free-talk/closing").get("closingReason").asString())
        .isEqualTo("USER_CONFIRMED");
    assertThat(requests.get("/api/v1/free-talk/closing").get("titleGenerationRequired").asBoolean())
        .isTrue();
    assertThat(opening.emotion()).isEqualTo(CharacterEmotion.HAPPY);
    assertThat(innerThought.innerThoughtType().name()).isEqualTo("GOOD");
    assertThat(closing.inferredTitle()).isEqualTo("Weekend Hiking");
    assertThat(closing.translatedMessage()).isEqualTo("이야기해서 좋았어.");
  }

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

  @Test
  void postsMemoryCandidateContractAndMapsSuccessfulResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/memory-candidates",
        requests,
        successResponse(memoryCandidateData("memory-candidate-v1", 0, "EVENT")));

    AiMemoryCandidatesResult result =
        remoteClient().extractMemoryCandidates(memoryCandidatesRequest());

    JsonNode request = requests.get("/api/v1/free-talk/memory-candidates");
    assertThat(request.get("sessionId").asLong()).isEqualTo(300L);
    assertThat(request.get("characterId").asText()).isEqualTo("chloe");
    assertThat(request.get("timezone").asText()).isEqualTo("Asia/Seoul");
    assertThat(request.get("conversationHistory").get(0).get("messageId").asLong())
        .isEqualTo(3001L);
    assertThat(request.get("conversationHistory").get(0).get("occurredAt").asText())
        .isEqualTo("2026-08-25T20:00:00+09:00");
    assertThat(result.extractorVersion()).isEqualTo("memory-candidate-v1");
    assertThat(result.candidates()).hasSize(1);
    assertThat(result.candidates().getFirst().embedding()).hasSize(1536);
    assertThat(result.candidates().getFirst().embeddingModel())
        .isEqualTo("openai/text-embedding-3-small");
  }

  @Test
  void postsMemoryResolutionContractAndMapsSuccessfulResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/memory-resolution",
        requests,
        successResponse(
            "{\"resolutions\":[{\"candidateIndex\":0,\"operation\":\"SUPERSEDE\","
                + "\"supersededMemoryIds\":[77]}]}"));

    AiMemoryResolutionResult result = remoteClient().resolveMemory(memoryResolutionRequest());

    JsonNode request = requests.get("/api/v1/free-talk/memory-resolution");
    assertThat(request.get("candidates").get(0).get("candidateIndex").asInt()).isZero();
    assertThat(request.get("candidates").get(0).get("observedAt").asText())
        .isEqualTo("2026-08-29T19:20:00+09:00");
    assertThat(
            request
                .get("candidates")
                .get(0)
                .get("comparableMemories")
                .get(0)
                .get("memoryId")
                .asLong())
        .isEqualTo(77L);
    assertThat(result.resolutions().getFirst().operation()).isEqualTo(AiMemoryOperation.SUPERSEDE);
    assertThat(result.resolutions().getFirst().supersededMemoryIds()).containsExactly(77L);
  }

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

  @Test
  void acceptsNullEmotionForConversationResponses() throws Exception {
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

    assertThat(remoteClient().generateOpening(openingRequest()).emotion()).isNull();
    assertThat(remoteClient().generateTurn(turnRequest()).emotion()).isNull();
    assertThat(remoteClient().generateClosing(closingRequest()).emotion()).isNull();
  }

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

  @Test
  void rejectsNullDataOnSuccessfulResponse() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/opening", 200, "{\"success\":true,\"data\":null,\"error\":null}");

    assertGenerationError(
        () -> remoteClient().generateOpening(openingRequest()), ErrorCode.AI_RESPONSE_INVALID);
  }

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
            Duration.ofSeconds(1)));
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

  private String learningContentData(
      String targetExpressionText, String meaning, String usageSummary, int practiceCount) {
    return "{\"expressions\":["
        + learningContentItem(targetExpressionText, meaning, usageSummary, practiceCount)
        + "]}";
  }

  private String learningContentItem(
      String targetExpressionText, String meaning, String usageSummary, int practiceCount) {
    String practiceExample =
        String.join(
            "\n",
            "{",
            "  \"imageUrl\": null,",
            "  \"sentenceText\": \"I'm up for hiking.\",",
            "  \"sentenceWords\": [\"I'm\", \"up\", \"for\", \"hiking\"],",
            "  \"highlightingPart\": \"I'm up for\",",
            "  \"practiceQuestion\": \"Want to hike?\",",
            "  \"sentenceTranslation\": \"등산하는 거 좋아.\",",
            "  \"sentenceWordChoices\": [\"hiking\", \"I'm\", \"up\", \"for\", \"to\"],",
            "  \"practiceQuestionTranslation\": \"등산 갈래?\"",
            "}");
    return String.join(
        "\n",
        "{",
        "  \"targetExpressionText\": \"%s\",".formatted(targetExpressionText),
        "  \"baseExpressionMeaningText\": \"%s\",".formatted(meaning),
        "  \"usageSummary\": \"%s\",".formatted(usageSummary),
        "  \"usageDescription\": \"친근한 제안에 동의할 때 사용합니다.\",",
        "  \"representativeQuestionText\": \"Want to go hiking?\",",
        "  \"representativeQuestionTranslation\": \"등산 갈래?\",",
        "  \"representativeSentenceText\": \"I'm up for that.\",",
        "  \"representativeSentenceTranslation\": \"좋아, 그거 하자.\",",
        "  \"representativeSentenceWords\": [\"I'm\", \"up\", \"for\", \"that\"],",
        "  \"representativeSentenceWordChoices\": [\"that\", \"I'm\", \"up\", \"for\", \"to\"],",
        "  \"representativeImageUrl\": null,",
        "  \"practiceExamples\": [%s]"
            .formatted(
                String.join(",", java.util.Collections.nCopies(practiceCount, practiceExample))),
        "}");
  }

  private AiFreeTalkOpeningRequest openingRequest() {
    return new AiFreeTalkOpeningRequest(
        300L,
        "chloe",
        "EN",
        "KR",
        new AiFreeTalkTopic(2L, "주말 계획", "Ask about the user's weekend plans."));
  }

  private AiFreeTalkTurnRequest turnRequest() {
    return new AiFreeTalkTurnRequest(
        300L, "chloe", 3002L, 1, "EN", "KR", AiFreeTalkResponseMode.NORMAL, true, null, history());
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

  private AiMemoryCandidatesRequest memoryCandidatesRequest() {
    return new AiMemoryCandidatesRequest(
        300L,
        "chloe",
        "EN",
        "KR",
        "Asia/Seoul",
        List.of(
            new AiConversationHistoryMessage(
                3001L,
                1,
                "AI",
                "How was your weekend?",
                "주말은 어땠어?",
                OffsetDateTime.parse("2026-08-25T20:00:00+09:00")),
            new AiConversationHistoryMessage(
                3002L,
                1,
                "USER",
                "I have an interview next Friday.",
                null,
                OffsetDateTime.parse("2026-08-25T20:10:00+09:00"))));
  }

  private AiMemoryResolutionRequest memoryResolutionRequest() {
    return new AiMemoryResolutionRequest(
        List.of(
            new AiMemoryResolutionRequest.Candidate(
                0,
                "사용자는 면접에 합격했다.",
                ConversationMemoryType.EVENT,
                List.of(3002L),
                OffsetDateTime.parse("2026-08-29T19:20:00+09:00"),
                List.of(
                    new AiMemoryResolutionRequest.ComparableMemory(
                        77L,
                        "사용자는 다음 주에 면접이 있다.",
                        OffsetDateTime.parse("2026-08-25T20:10:00+09:00"),
                        null,
                        OffsetDateTime.parse("2026-08-25T20:10:00+09:00"))))));
  }

  private String memoryCandidateData(
      String extractorVersion, int candidateIndex, String memoryType) {
    return "{\"extractorVersion\":\""
        + extractorVersion
        + "\",\"candidates\":["
        + memoryCandidateJson(candidateIndex, memoryType)
        + "]}";
  }

  private String memoryCandidateJson(int candidateIndex, String memoryType) {
    return "{\"candidateIndex\":"
        + candidateIndex
        + ",\"memoryType\":\""
        + memoryType
        + "\",\"content\":\"사용자는 2026년 8월 28일에 면접이 있다.\","
        + "\"contentLocale\":\"KR\",\"sourceMessageIds\":[3002],"
        + "\"confidence\":0.94,"
        + "\"validFrom\":\"2026-08-25T20:10:00+09:00\",\"validTo\":null,"
        + "\"embeddingModel\":\"openai/text-embedding-3-small\",\"embedding\":"
        + embeddingJson()
        + "}";
  }

  private String embeddingJson() {
    return "[" + String.join(",", java.util.Collections.nCopies(1536, "0.0")) + "]";
  }
}
