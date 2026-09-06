// 로컬 개발과 통합 테스트에서 사용할 결정적 프리톡 AI 대체 클라이언트다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 로컬 개발과 통합 테스트에서 사용할 결정적 프리톡 AI 대체 클라이언트다. */
@Component
@ConditionalOnProperty(
    prefix = "landit.ai",
    name = "client-mode",
    havingValue = "local",
    matchIfMissing = true)
public class LocalAiFreeTalkClient implements AiFreeTalkClient {

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request) {
    return new AiFreeTalkOpeningResult(
        "What would you like to talk about today?",
        "오늘은 무슨 이야기를 하고 싶어?",
        CharacterEmotion.HAPPY,
        List.of());
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request) {
    return new AiFreeTalkTurnResult(
        false,
        null,
        "That sounds interesting. Tell me more.",
        "흥미롭다. 조금 더 이야기해줘.",
        CharacterEmotion.HAPPY,
        List.of());
  }

  /** {@inheritDoc} */
  @Override
  public AiMemoryQueryEmbeddingResult embedMemoryQuery(AiMemoryQueryEmbeddingRequest request) {
    return new AiMemoryQueryEmbeddingResult("openai/text-embedding-3-small", firstAxisEmbedding());
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkInnerThoughtResult generateInnerThought(AiFreeTalkInnerThoughtRequest request) {
    String innerThought = "사용자가 대화를 자연스럽게 이어가고 있다.";
    return new AiFreeTalkInnerThoughtResult(innerThought, InnerThoughtType.GOOD);
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request) {
    return new AiFreeTalkClosingResult(
        request.titleGenerationRequired() ? "오늘의 프리톡" : null,
        "It was nice talking with you.",
        "이야기해서 좋았어.",
        CharacterEmotion.NEUTRAL);
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkExpressionRecommendationsResult recommendExpressions(
      AiFreeTalkExpressionRecommendationsRequest request) {
    if (request.existingExpressions().isEmpty()) {
      return new AiFreeTalkExpressionRecommendationsResult(List.of());
    }
    return new AiFreeTalkExpressionRecommendationsResult(
        List.of(
            new AiFreeTalkExpressionRecommendation(
                1, request.existingExpressions().getFirst().expressionId())));
  }

  /** {@inheritDoc} */
  @Override
  public AiConversationEmbeddingsResult extractConversationEmbeddings(
      AiConversationEmbeddingsRequest request) {
    return new AiConversationEmbeddingsResult(
        List.of(new AiConversationExcerpt("That sounds interesting.", firstAxisEmbedding())));
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
    Float[] embedding = new Float[AiConversationExcerpt.EMBEDDING_DIMENSION];
    Arrays.fill(embedding, 0.0f);
    embedding[0] = 1.0f;
    return List.of(embedding);
  }
}
