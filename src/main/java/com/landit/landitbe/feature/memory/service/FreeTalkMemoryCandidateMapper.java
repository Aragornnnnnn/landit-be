// 프리톡 장기기억 후보 응답을 저장 모델과 resolution 입력으로 변환한다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.session.service.FreeTalkMemoryGenerationContextService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class FreeTalkMemoryCandidateMapper {

  private static final int MAX_CANDIDATES = 5;
  private static final int EMBEDDING_DIMENSION = 1536;
  private static final String EMBEDDING_MODEL = "openai/text-embedding-3-small";

  private final Clock clock;

  List<FreeTalkMemoryCandidate> mapCandidates(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      AiMemoryCandidatesResult extraction) {
    validateExtractionResult(extraction);
    Map<Long, AiConversationHistoryMessage> historyById = historyById(context.history());
    return extraction.candidates().stream()
        .map(
            candidate ->
                mapCandidate(context, candidate, extraction.extractorVersion(), historyById))
        .toList();
  }

  private FreeTalkMemoryCandidate mapCandidate(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      AiMemoryCandidatesResult.Candidate candidate,
      String extractorVersion,
      Map<Long, AiConversationHistoryMessage> historyById) {
    validateSourceMessageIds(candidate);
    List<AiConversationHistoryMessage> sources =
        candidate.sourceMessageIds().stream().map(historyById::get).toList();
    if (sources.stream().anyMatch(message -> message == null || !"USER".equals(message.role()))) {
      throw new IllegalArgumentException("장기기억 원본은 사용자 메시지만 허용됩니다.");
    }
    validateCandidateContract(context, candidate);

    OffsetDateTime observedAt = latestObservedAt(sources);
    LocalDateTime observedAtLocal = observedAt.atZoneSameInstant(clock.getZone()).toLocalDateTime();
    NewConversationMemory memory = toMemory(context, candidate, observedAtLocal, extractorVersion);
    return new FreeTalkMemoryCandidate(
        candidate.candidateIndex(),
        memory,
        toResolutionCandidate(candidate, observedAt),
        List.of());
  }

  private static OffsetDateTime latestObservedAt(List<AiConversationHistoryMessage> sources) {
    return sources.stream()
        .map(AiConversationHistoryMessage::occurredAt)
        .max((left, right) -> left.toInstant().compareTo(right.toInstant()))
        .orElseThrow();
  }

  private NewConversationMemory toMemory(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      AiMemoryCandidatesResult.Candidate candidate,
      LocalDateTime observedAt,
      String extractorVersion) {
    String characterId =
        candidate.memoryType() == ConversationMemoryType.PROFILE ? null : context.characterId();
    return new NewConversationMemory(
        context.userProfileId(),
        characterId,
        candidate.memoryType(),
        candidate.content(),
        toJavaLocale(context.baseLocale()),
        candidate.confidence(),
        toLocalDateTime(candidate.validFrom(), observedAt),
        toLocalDateTime(candidate.validTo(), null),
        observedAt,
        LocalDateTime.now(clock),
        extractorVersion,
        candidate.embeddingModel(),
        candidate.embedding());
  }

  private static AiMemoryResolutionRequest.Candidate toResolutionCandidate(
      AiMemoryCandidatesResult.Candidate candidate, OffsetDateTime observedAt) {
    return new AiMemoryResolutionRequest.Candidate(
        candidate.candidateIndex(),
        candidate.content(),
        candidate.memoryType(),
        candidate.sourceMessageIds(),
        observedAt,
        List.of());
  }

  private static void validateSourceMessageIds(AiMemoryCandidatesResult.Candidate candidate) {
    if (candidate == null
        || candidate.sourceMessageIds() == null
        || candidate.sourceMessageIds().isEmpty()
        || candidate.sourceMessageIds().stream().anyMatch(id -> id == null || id <= 0)
        || candidate.sourceMessageIds().size()
            != new HashSet<>(candidate.sourceMessageIds()).size()) {
      throw new IllegalArgumentException("장기기억 원본 메시지 목록이 유효하지 않습니다.");
    }
  }

  private static void validateCandidateContract(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      AiMemoryCandidatesResult.Candidate candidate) {
    if (candidate.candidateIndex() == null
        || candidate.confidence() == null
        || candidate.memoryType() == null
        || candidate.content() == null
        || candidate.content().isBlank()
        || candidate.content().length() > 500
        || !context.baseLocale().equals(candidate.contentLocale())
        || !Double.isFinite(candidate.confidence())
        || candidate.confidence() < 0
        || candidate.confidence() > 1
        || invalidValidityRange(candidate.validFrom(), candidate.validTo())
        || !EMBEDDING_MODEL.equals(candidate.embeddingModel())
        || invalidEmbedding(candidate.embedding())) {
      throw new IllegalArgumentException("장기기억 후보 계약이 유효하지 않습니다.");
    }
  }

  private static Map<Long, AiConversationHistoryMessage> historyById(
      List<AiConversationHistoryMessage> history) {
    if (history == null) {
      throw new IllegalArgumentException("장기기억 생성 이력이 필요합니다.");
    }
    Map<Long, AiConversationHistoryMessage> byId = new HashMap<>();
    for (AiConversationHistoryMessage message : history) {
      if (message == null
          || message.messageId() == null
          || message.role() == null
          || message.occurredAt() == null
          || byId.put(message.messageId(), message) != null) {
        throw new IllegalArgumentException("장기기억 원본 이력이 유효하지 않습니다.");
      }
    }
    return byId;
  }

  private static void validateExtractionResult(AiMemoryCandidatesResult result) {
    if (result == null
        || result.extractorVersion() == null
        || result.extractorVersion().isBlank()
        || result.candidates() == null
        || result.candidates().size() > MAX_CANDIDATES) {
      throw new IllegalArgumentException("장기기억 후보 추출 응답이 유효하지 않습니다.");
    }
    for (int index = 0; index < result.candidates().size(); index++) {
      AiMemoryCandidatesResult.Candidate candidate = result.candidates().get(index);
      if (candidate == null
          || candidate.candidateIndex() == null
          || candidate.candidateIndex() != index) {
        throw new IllegalArgumentException("장기기억 후보 인덱스가 연속적이지 않습니다.");
      }
    }
  }

  private static boolean invalidEmbedding(List<Float> embedding) {
    return embedding == null
        || embedding.size() != EMBEDDING_DIMENSION
        || embedding.stream().anyMatch(value -> value == null || !Float.isFinite(value));
  }

  private LocalDateTime toLocalDateTime(OffsetDateTime value, LocalDateTime defaultValue) {
    return value == null
        ? defaultValue
        : value.atZoneSameInstant(clock.getZone()).toLocalDateTime();
  }

  private static boolean invalidValidityRange(OffsetDateTime validFrom, OffsetDateTime validTo) {
    return validFrom != null && validTo != null && validTo.isBefore(validFrom);
  }

  private static Locale toJavaLocale(String baseLocale) {
    return switch (baseLocale) {
      case "EN" -> Locale.ENGLISH;
      case "KR" -> Locale.KOREAN;
      default -> throw new IllegalArgumentException("기준 locale이 유효하지 않습니다.");
    };
  }
}
