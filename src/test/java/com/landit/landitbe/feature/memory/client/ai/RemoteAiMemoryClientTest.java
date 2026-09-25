// 원격 기억 AI의 요청·응답과 임베딩 타임아웃 계약을 검증한다.

package com.landit.landitbe.feature.memory.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingResult;
import com.landit.landitbe.shared.client.ai.AiHttpClient;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 기억 AI의 HTTP 계약을 검증한다. */
class RemoteAiMemoryClientTest {
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

  @DisplayName("기억 후보 추출 계약을 AI에 전송하고 성공 응답을 변환한다.")
  @Test
  void postsMemoryCandidateContractAndMapsSuccessfulResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/memory-candidates",
        requests,
        successResponse(memoryCandidateData("memory-candidate-v1", 0, "EVENT")));

    AiMemoryCandidatesResult result =
        memoryClient().extractMemoryCandidates(memoryCandidatesRequest());

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

  @DisplayName("기억 후보 응답에 실려 온 후속 질문을 읽고, 그 밖의 모르는 필드는 무시한다.")
  @Test
  void readsFollowUpQuestionAndIgnoresUnknownFields() throws Exception {
    String candidatesWithFollowUp =
        "{\"extractorVersion\":\"memory-candidate-v10\",\"candidates\":["
            + memoryCandidateJson(0, "EVENT")
            + "],\"followUpQuestion\":{\"memoryId\":null,\"candidateIndex\":0,"
            + "\"triggerType\":\"PAST_EVENT\",\"question\":\"면접 어떻게 됐어?\","
            + "\"invite\":\"다음엔 그 얘기 하자.\",\"addedLater\":1},\"addedLater\":{\"x\":1}}";
    registerJsonResponse(
        "/api/v1/free-talk/memory-candidates",
        new ConcurrentHashMap<>(),
        successResponse(candidatesWithFollowUp));

    AiMemoryCandidatesResult result =
        memoryClient().extractMemoryCandidates(memoryCandidatesRequest());

    assertThat(result.candidates()).hasSize(1);
    assertThat(result.followUpQuestion())
        .isEqualTo(
            new AiMemoryCandidatesResult.FollowUpQuestion(
                null, 0, "PAST_EVENT", "면접 어떻게 됐어?", "다음엔 그 얘기 하자."));
  }

  @DisplayName("모르는 계기 값이 와도 기억 후보 응답 전체의 변환은 실패하지 않는다.")
  @Test
  void keepsUnknownTriggerTypeAsTextWithoutFailingCandidates() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/memory-candidates",
        new ConcurrentHashMap<>(),
        successResponse(
            "{\"extractorVersion\":\"memory-candidate-v10\",\"candidates\":[],"
                + "\"followUpQuestion\":{\"memoryId\":42,\"candidateIndex\":null,"
                + "\"triggerType\":\"BRAND_NEW\",\"question\":\"q\",\"invite\":\"i\"}}"));

    AiMemoryCandidatesResult result =
        memoryClient().extractMemoryCandidates(memoryCandidatesRequest());

    assertThat(result.followUpQuestion().triggerType()).isEqualTo("BRAND_NEW");
  }

  @DisplayName("후속 질문 문맥이 있으면 기존 기억·이미 쓴 기억·종료 방식을 보내고, 없으면 그 필드들을 싣지 않는다.")
  @Test
  void sendsFollowUpContextOnlyWhenPresent() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/memory-candidates",
        requests,
        successResponse(memoryCandidateData("memory-candidate-v10", 0, "EVENT")));
    AiMemoryCandidatesRequest plain = memoryCandidatesRequest();

    memoryClient().extractMemoryCandidates(plain);

    JsonNode plainRequest = requests.get("/api/v1/free-talk/memory-candidates");
    // 이 필드들을 모르는 구버전 AI 서버가 보낼 것이 없는 요청까지 거부하지 않게 한다.
    assertThat(plainRequest.has("existingMemories")).isFalse();
    assertThat(plainRequest.has("askedMemoryIds")).isFalse();
    assertThat(plainRequest.has("sessionEndedBy")).isFalse();

    memoryClient()
        .extractMemoryCandidates(
            new AiMemoryCandidatesRequest(
                plain.sessionId(),
                plain.characterId(),
                plain.targetLocale(),
                plain.baseLocale(),
                plain.timezone(),
                plain.conversationHistory(),
                List.of(
                    new com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext(
                        42L, ConversationMemoryType.EVENT, "다음 주에 면접이 있다.")),
                List.of(7L, 9L),
                "TIME_LIMIT_REACHED"));

    JsonNode request = requests.get("/api/v1/free-talk/memory-candidates");
    assertThat(request.get("existingMemories").get(0).get("memoryId").asLong()).isEqualTo(42L);
    assertThat(request.get("existingMemories").get(0).get("memoryType").asText())
        .isEqualTo("EVENT");
    assertThat(request.get("askedMemoryIds")).hasSize(2);
    assertThat(request.get("askedMemoryIds").get(1).asLong()).isEqualTo(9L);
    assertThat(request.get("sessionEndedBy").asText()).isEqualTo("TIME_LIMIT_REACHED");
  }

  @DisplayName("기억 충돌 해결 계약을 AI에 전송하고 성공 응답을 변환한다.")
  @Test
  void postsMemoryResolutionContractAndMapsSuccessfulResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/memory-resolution",
        requests,
        successResponse(
            "{\"resolutions\":[{\"candidateIndex\":0,\"operation\":\"SUPERSEDE\","
                + "\"supersededMemoryIds\":[77]}]}"));

    AiMemoryResolutionResult result = memoryClient().resolveMemory(memoryResolutionRequest());

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
    JsonNode source = request.get("candidates").get(0).get("sourceMessages").get(0);
    assertThat(source.get("messageId").asLong()).isEqualTo(3002L);
    assertThat(source.get("role").asText()).isEqualTo("USER");
    assertThat(source.get("content").asText()).isEqualTo("I passed the interview.");
    assertThat(source.get("occurredAt").asText()).isEqualTo("2026-08-29T19:20:00+09:00");
    assertThat(result.resolutions().getFirst().operation()).isEqualTo(AiMemoryOperation.SUPERSEDE);
    assertThat(result.resolutions().getFirst().supersededMemoryIds()).containsExactly(77L);
  }

  @DisplayName("기억 검색 임베딩 계약을 전송하고 정해진 차원의 벡터를 반환한다.")
  @Test
  void postsMemoryQueryEmbeddingContractAndMapsFixedDimensionVector() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/memory-query-embedding",
        requests,
        successResponse(
            "{\"embeddingModel\":\"openai/text-embedding-3-small\",\"embedding\":"
                + embeddingJson()
                + "}"));

    AiMemoryQueryEmbeddingResult result =
        memoryClient().embedMemoryQuery(new AiMemoryQueryEmbeddingRequest(" weekend plans "));

    assertThat(requests.get("/api/v1/free-talk/memory-query-embedding").get("query").asText())
        .isEqualTo(" weekend plans ");
    assertThat(result.embeddingModel()).isEqualTo("openai/text-embedding-3-small");
    assertThat(result.embedding()).hasSize(1536);
  }

  @DisplayName("기억 조회의 성공 응답이 늦으면 응답 전에 시간 초과로 처리한다.")
  @Test
  void memoryQueryTimesOutBeforeDelayedSuccessfulResponse() {
    registerDelayedResponse(
        "/api/v1/free-talk/memory-query-embedding",
        3_000,
        successResponse(
            "{\"embeddingModel\":\"openai/text-embedding-3-small\",\"embedding\":"
                + embeddingJson()
                + "}"));
    long started = System.nanoTime();
    assertGenerationError(
        () ->
            memoryClient(Duration.ofSeconds(5))
                .embedMemoryQuery(new AiMemoryQueryEmbeddingRequest("weekend plans")),
        ErrorCode.AI_GENERATION_FAILED);
    assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(2_900));
  }

  @Test
  @DisplayName("기억 후보와 판정은 일반 대화와 별개의 55초·25초 제한을 전달한다.")
  void usesDedicatedMemoryTimeouts() {
    try (var construction = mockConstruction(AiHttpClient.class)) {
      RemoteAiMemoryClient client = memoryClient();
      AiMemoryCandidatesRequest candidates = memoryCandidatesRequest();
      AiMemoryResolutionRequest resolution = memoryResolutionRequest();
      client.extractMemoryCandidates(candidates);
      client.resolveMemory(resolution);
      AiHttpClient http = construction.constructed().getFirst();
      verify(http)
          .post(
              "/api/v1/free-talk/memory-candidates",
              candidates,
              AiMemoryCandidatesResult.class,
              Duration.ofSeconds(55));
      verify(http)
          .post(
              "/api/v1/free-talk/memory-resolution",
              resolution,
              AiMemoryResolutionResult.class,
              Duration.ofSeconds(25));
    }
  }

  @Test
  @DisplayName("기억 생성은 짧게 설정된 일반 대화 제한과 독립적으로 응답을 기다린다.")
  void waitsForMemoryIndependentlyOfGeneralTimeout() {
    registerDelayedResponse(
        "/api/v1/free-talk/memory-candidates",
        200,
        successResponse("{\"extractorVersion\":\"v11\",\"candidates\":[]}"));
    assertThat(
            memoryClient(Duration.ofMillis(50))
                .extractMemoryCandidates(memoryCandidatesRequest())
                .candidates())
        .isEmpty();
  }

  @Test
  @DisplayName("HTTP 헤더가 도착해도 응답 본문이 지연되면 전체 제한 시간에 실패한다.")
  void timesOutWhileReceivingBody() {
    server.createContext(
        "/slow-body",
        exchange -> {
          byte[] body = successResponse("{}").getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          try {
            exchange.getResponseBody().write(body, 0, 1);
            exchange.getResponseBody().flush();
            Thread.sleep(700);
            exchange.getResponseBody().write(body, 1, body.length - 1);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    AiClientProperties properties =
        new AiClientProperties(
            "http://localhost:" + server.getAddress().getPort(),
            "remote",
            "KOREAN_LEARNER",
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5));
    long start = System.nanoTime();
    assertGenerationError(
        () ->
            new AiHttpClient(jsonMapper, properties)
                .post("/slow-body", Map.of(), Map.class, Duration.ofMillis(150)),
        ErrorCode.AI_GENERATION_FAILED);
    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(600));
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

  private String successResponse(String data) {
    return "{\"success\":true,\"data\":" + data + ",\"error\":null}";
  }

  private AiMemoryCandidatesRequest memoryCandidatesRequest() {
    return new AiMemoryCandidatesRequest(
        300L,
        "chloe",
        "EN",
        "KR",
        "Asia/Seoul",
        List.of(
            new ConversationMemoryHistoryMessage(
                3001L,
                1,
                "AI",
                "How was your weekend?",
                "주말은 어땠어?",
                OffsetDateTime.parse("2026-08-25T20:00:00+09:00")),
            new ConversationMemoryHistoryMessage(
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
                List.of(
                    new ConversationMemoryHistoryMessage(
                        3002L,
                        1,
                        "USER",
                        "I passed the interview.",
                        null,
                        OffsetDateTime.parse("2026-08-29T19:20:00+09:00"))),
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

  private RemoteAiMemoryClient memoryClient() {
    return memoryClient(Duration.ofSeconds(1));
  }

  private RemoteAiMemoryClient memoryClient(Duration requestTimeout) {
    return new RemoteAiMemoryClient(
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
}
