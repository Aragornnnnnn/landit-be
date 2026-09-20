// 로컬 개발과 통합 테스트에서 사용할 결정적 기억 AI 대체 클라이언트다.

package com.landit.landitbe.feature.memory.client.ai;

import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingResult;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 로컬 개발과 통합 테스트에서 사용할 결정적 기억 AI 대체 클라이언트다. */
@Component
@ConditionalOnProperty(
    prefix = "landit.ai",
    name = "client-mode",
    havingValue = "local",
    matchIfMissing = true)
public class LocalAiMemoryClient implements AiMemoryClient {

  /** {@inheritDoc} */
  @Override
  public AiMemoryQueryEmbeddingResult embedMemoryQuery(AiMemoryQueryEmbeddingRequest request) {
    return new AiMemoryQueryEmbeddingResult("openai/text-embedding-3-small", firstAxisEmbedding());
  }

  /** {@inheritDoc} */
  @Override
  public AiMemoryCandidatesResult extractMemoryCandidates(AiMemoryCandidatesRequest request) {
    return new AiMemoryCandidatesResult("memory-candidate-v1", List.of());
  }

  /** {@inheritDoc} */
  @Override
  public AiMemoryResolutionResult resolveMemory(AiMemoryResolutionRequest request) {
    return new AiMemoryResolutionResult(
        request.candidates().stream()
            .map(
                candidate ->
                    new AiMemoryResolutionResult.Resolution(
                        candidate.candidateIndex(), AiMemoryOperation.ADD, List.of()))
            .toList());
  }

  // 테스트에서 예측할 수 있도록 첫 성분만 1인 고정 임베딩을 만든다.
  private static List<Float> firstAxisEmbedding() {
    Float[] embedding = new Float[1536];
    Arrays.fill(embedding, 0.0f);
    embedding[0] = 1.0f;
    return List.of(embedding);
  }
}
