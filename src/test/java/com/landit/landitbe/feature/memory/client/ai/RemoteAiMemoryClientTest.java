// 원격 기억 AI의 요청·응답과 임베딩 타임아웃 계약을 검증한다.

package com.landit.landitbe.feature.memory.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingResult;
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
