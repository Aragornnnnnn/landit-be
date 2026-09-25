// 장기기억 검색·후보 추출·판정의 원격 AI 계약을 구현한다.

package com.landit.landitbe.feature.memory.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingResult;
import com.landit.landitbe.shared.client.ai.AiHttpClient;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** 원격 AI 호출 계약을 구현한다. */
@Component
@ConditionalOnProperty(prefix = "landit.ai", name = "client-mode", havingValue = "remote")
public class RemoteAiMemoryClient implements AiMemoryClient {
  private final AiHttpClient http;

  private static final String MEMORY_QUERY_EMBEDDING_PATH =
      "/api/v1/free-talk/memory-query-embedding";
  private static final String MEMORY_CANDIDATES_PATH = "/api/v1/free-talk/memory-candidates";
  private static final String MEMORY_RESOLUTION_PATH = "/api/v1/free-talk/memory-resolution";
  private static final Duration MEMORY_QUERY_TIMEOUT = Duration.ofSeconds(2);
  // AI 내부 후보 생성 50초·판정 20초 예산에 HTTP 전달 여유 5초를 둔다.
  private static final Duration MEMORY_CANDIDATES_TIMEOUT = Duration.ofSeconds(55);
  private static final Duration MEMORY_RESOLUTION_TIMEOUT = Duration.ofSeconds(25);

  /**
   * JSON 변환기와 AI 서버 설정으로 원격 기억 클라이언트를 구성한다.
   *
   * @param jsonMapper AI 요청과 응답 JSON 변환기
   * @param properties AI 서버 연결 설정
   */
  public RemoteAiMemoryClient(JsonMapper jsonMapper, AiClientProperties properties) {
    this.http = new AiHttpClient(jsonMapper, properties);
  }

  /** {@inheritDoc} */
  @Override
  public AiMemoryQueryEmbeddingResult embedMemoryQuery(AiMemoryQueryEmbeddingRequest request) {
    return http.post(
            MEMORY_QUERY_EMBEDDING_PATH,
            request,
            RemoteMemoryQueryEmbeddingResponse.class,
            MEMORY_QUERY_TIMEOUT)
        .toResult();
  }

  /** {@inheritDoc} */
  @Override
  public AiMemoryCandidatesResult extractMemoryCandidates(AiMemoryCandidatesRequest request) {
    return http.post(
        MEMORY_CANDIDATES_PATH, request, AiMemoryCandidatesResult.class, MEMORY_CANDIDATES_TIMEOUT);
  }

  /** {@inheritDoc} */
  @Override
  public AiMemoryResolutionResult resolveMemory(AiMemoryResolutionRequest request) {
    return http.post(
        MEMORY_RESOLUTION_PATH, request, AiMemoryResolutionResult.class, MEMORY_RESOLUTION_TIMEOUT);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteMemoryQueryEmbeddingResponse(String embeddingModel, List<Float> embedding) {

    /** 원격 query embedding이 차원·유한값 계약을 지키는지 검증한다. */
    private AiMemoryQueryEmbeddingResult toResult() {
      if (blank(embeddingModel)
          || embedding == null
          || embedding.size() != 1536
          || embedding.stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiMemoryQueryEmbeddingResult(embeddingModel, embedding);
    }
  }

  // 임베딩 모델 이름이 비어 있는지 확인한다.
  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
