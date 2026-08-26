// 완료 프리톡의 장기기억 후보를 비교하고 원자 저장까지 오케스트레이션한다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.session.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.session.service.FreeTalkMemoryGenerationContextService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 완료 프리톡의 장기기억 후보를 비교하고 원자 저장까지 오케스트레이션한다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkMemoryGenerationService {

  private static final int MAX_CANDIDATES = 5;
  private static final int MAX_COMPARABLE_MEMORIES = 3;
  private static final int EMBEDDING_DIMENSION = 1536;
  private static final String EMBEDDING_MODEL = "openai/text-embedding-3-small";

  private final FreeTalkMemoryGenerationContextService contextService;
  private final AiFreeTalkClient aiClient;
  private final ConversationMemorySearchRepository searchRepository;
  private final ConversationMemoryWriteService writeService;
  private final Clock clock;

  /** 완료된 프리톡의 장기기억 생성을 한 번 실행한다. */
  public void generate(long learningSessionId) {
    FreeTalkMemoryGenerationContextService.GenerationContext context;
    try {
      context = contextService.claim(learningSessionId);
    } catch (RuntimeException exception) {
      failSafely(learningSessionId, exception);
      return;
    }
    if (context == null) {
      return;
    }

    try {
      AiMemoryCandidatesResult extraction =
          aiClient.extractMemoryCandidates(
              new AiMemoryCandidatesRequest(
                  context.learningSessionId(),
                  context.characterId(),
                  context.targetLocale(),
                  context.baseLocale(),
                  context.timezone(),
                  context.history()));
      validateExtractionResult(extraction);
      List<ConversationMemoryResolutionPlan> plans = buildPlans(context, extraction);
      if (writeService.persistIfSnapshotCurrent(learningSessionId, context.userProfileId(), plans)
          == ConversationMemoryWriteService.PersistenceResult.STALE) {
        throw new IllegalStateException("장기기억 비교 snapshot이 변경됐습니다.");
      }
    } catch (RuntimeException exception) {
      failSafely(learningSessionId, exception);
    }
  }

  /** 작업 제출 실패 등으로 실행되지 못한 작업을 조건부 실패 상태로 전환한다. */
  public void markFailed(long learningSessionId) {
    failSafely(learningSessionId, null);
  }

  private void failSafely(long learningSessionId, RuntimeException cause) {
    try {
      contextService.fail(learningSessionId);
    } catch (RuntimeException compensationFailure) {
      log.warn("프리톡 장기기억 실패 상태 전환도 실패했습니다. learningSessionId={}", learningSessionId);
    }
    if (cause != null) {
      log.warn("프리톡 장기기억 생성에 실패했습니다. learningSessionId={}", learningSessionId);
    }
  }

  private List<ConversationMemoryResolutionPlan> buildPlans(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      AiMemoryCandidatesResult extraction) {
    Map<Long, AiConversationHistoryMessage> historyById = historyById(context.history());
    List<CandidateData> candidates =
        new ArrayList<>(
            extraction.candidates().stream()
                .map(
                    candidate ->
                        candidateData(
                            context, candidate, extraction.extractorVersion(), historyById))
                .toList());
    if (candidates.isEmpty()) {
      return List.of();
    }
    for (int index = 0; index < candidates.size(); index++) {
      CandidateData candidate = candidates.get(index);
      List<ConversationMemoryMatch> comparable =
          searchRepository.searchActiveComparable(
              candidate.memory().embedding(),
              context.userProfileId(),
              candidate.memory().characterId(),
              candidate.memory().memoryType(),
              MAX_COMPARABLE_MEMORIES);
      if (comparable == null || comparable.size() > MAX_COMPARABLE_MEMORIES) {
        throw new IllegalArgumentException("비교 장기기억 목록이 유효하지 않습니다.");
      }
      candidates.set(index, candidate.withComparable(comparable, clock.getZone()));
    }
    if (candidates.size() == 1 && candidates.getFirst().comparableMemories().isEmpty()) {
      CandidateData candidate = candidates.getFirst();
      return List.of(
          new ConversationMemoryResolutionPlan(
              candidate.memory(),
              candidate.resolutionCandidate().sourceMessageIds(),
              List.of(),
              AiMemoryOperation.ADD,
              List.of()));
    }
    AiMemoryResolutionResult resolution =
        aiClient.resolveMemory(
            new AiMemoryResolutionRequest(
                candidates.stream().map(CandidateData::resolutionCandidate).toList()));
    return plansFromResolutions(candidates, resolution);
  }

  private List<ConversationMemoryResolutionPlan> plansFromResolutions(
      List<CandidateData> candidates, AiMemoryResolutionResult result) {
    if (result == null
        || result.resolutions() == null
        || result.resolutions().size() != candidates.size()) {
      throw new IllegalArgumentException("장기기억 상태 판정 응답 개수가 유효하지 않습니다.");
    }
    Map<Integer, CandidateData> candidatesByIndex = new HashMap<>();
    for (CandidateData candidate : candidates) {
      if (candidatesByIndex.put(candidate.candidateIndex(), candidate) != null) {
        throw new IllegalArgumentException("장기기억 후보 인덱스가 중복됐습니다.");
      }
    }
    Map<Integer, AiMemoryResolutionResult.Resolution> resolutions = new HashMap<>();
    Set<Long> supersededAcrossCandidates = new HashSet<>();
    for (AiMemoryResolutionResult.Resolution resolution : result.resolutions()) {
      if (resolution == null
          || resolution.candidateIndex() == null
          || resolution.operation() == null
          || resolution.supersededMemoryIds() == null
          || candidatesByIndex.get(resolution.candidateIndex()) == null
          || resolutions.put(resolution.candidateIndex(), resolution) != null) {
        throw new IllegalArgumentException("장기기억 상태 판정 응답이 유효하지 않습니다.");
      }
      CandidateData candidate = candidatesByIndex.get(resolution.candidateIndex());
      Set<Long> comparableIds =
          candidate.comparableMemories().stream()
              .map(ConversationMemoryMatch::memoryId)
              .collect(java.util.stream.Collectors.toSet());
      if (invalidSupersededIds(
          resolution.operation(),
          resolution.supersededMemoryIds(),
          comparableIds,
          supersededAcrossCandidates)) {
        throw new IllegalArgumentException("장기기억 대체 대상이 유효하지 않습니다.");
      }
    }
    if (resolutions.size() != candidates.size()) {
      throw new IllegalArgumentException("모든 장기기억 후보의 상태 판정이 필요합니다.");
    }
    return candidates.stream()
        .map(
            candidate -> {
              AiMemoryResolutionResult.Resolution resolution =
                  resolutions.get(candidate.candidateIndex());
              return new ConversationMemoryResolutionPlan(
                  candidate.memory(),
                  candidate.resolutionCandidate().sourceMessageIds(),
                  candidate.comparableMemories().stream()
                      .map(ConversationMemoryMatch::memoryId)
                      .toList(),
                  resolution.operation(),
                  resolution.supersededMemoryIds());
            })
        .toList();
  }

  private static boolean invalidSupersededIds(
      AiMemoryOperation operation, List<Long> ids, Set<Long> comparableIds, Set<Long> globalIds) {
    if (ids.stream().anyMatch(id -> id == null || id <= 0)
        || ids.size() != new HashSet<>(ids).size()
        || !comparableIds.containsAll(ids)
        || ids.stream().anyMatch(id -> !globalIds.add(id))) {
      return true;
    }
    return operation == AiMemoryOperation.SUPERSEDE ? ids.isEmpty() : !ids.isEmpty();
  }

  private CandidateData candidateData(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      AiMemoryCandidatesResult.Candidate candidate,
      String extractorVersion,
      Map<Long, AiConversationHistoryMessage> historyById) {
    if (candidate == null
        || candidate.sourceMessageIds() == null
        || candidate.sourceMessageIds().isEmpty()
        || candidate.sourceMessageIds().stream().anyMatch(id -> id == null || id <= 0)
        || candidate.sourceMessageIds().size()
            != new HashSet<>(candidate.sourceMessageIds()).size()) {
      throw new IllegalArgumentException("장기기억 원본 메시지 목록이 유효하지 않습니다.");
    }
    List<AiConversationHistoryMessage> sources =
        candidate.sourceMessageIds().stream().map(historyById::get).toList();
    if (sources.stream().anyMatch(message -> message == null || !"USER".equals(message.role()))) {
      throw new IllegalArgumentException("장기기억 원본은 사용자 메시지만 허용됩니다.");
    }
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
    OffsetDateTime observedAt =
        sources.stream()
            .map(AiConversationHistoryMessage::occurredAt)
            .max((left, right) -> left.toInstant().compareTo(right.toInstant()))
            .orElseThrow();
    LocalDateTime observedAtLocal = observedAt.atZoneSameInstant(clock.getZone()).toLocalDateTime();
    LocalDateTime validFrom =
        candidate.validFrom() == null
            ? observedAtLocal
            : candidate.validFrom().atZoneSameInstant(clock.getZone()).toLocalDateTime();
    LocalDateTime validTo =
        candidate.validTo() == null
            ? null
            : candidate.validTo().atZoneSameInstant(clock.getZone()).toLocalDateTime();
    String characterId =
        candidate.memoryType() == ConversationMemoryType.PROFILE ? null : context.characterId();
    NewConversationMemory memory =
        new NewConversationMemory(
            context.userProfileId(),
            characterId,
            candidate.memoryType(),
            candidate.content(),
            toJavaLocale(context.baseLocale()),
            candidate.confidence(),
            validFrom,
            validTo,
            observedAtLocal,
            LocalDateTime.now(clock),
            extractorVersion,
            candidate.embeddingModel(),
            candidate.embedding());
    return new CandidateData(
        candidate.candidateIndex(),
        memory,
        new AiMemoryResolutionRequest.Candidate(
            candidate.candidateIndex(),
            candidate.content(),
            candidate.memoryType(),
            candidate.sourceMessageIds(),
            observedAt,
            List.of()),
        List.of());
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

  private record CandidateData(
      int candidateIndex,
      NewConversationMemory memory,
      AiMemoryResolutionRequest.Candidate resolutionCandidate,
      List<ConversationMemoryMatch> comparableMemories) {

    private CandidateData {
      comparableMemories = List.copyOf(comparableMemories);
    }

    private CandidateData withComparable(List<ConversationMemoryMatch> comparable, ZoneId zoneId) {
      Set<Long> ids = new HashSet<>();
      for (ConversationMemoryMatch memory : comparable) {
        if (memory == null
            || memory.memoryId() <= 0
            || !ids.add(memory.memoryId())
            || memory.content() == null
            || memory.content().isBlank()
            || memory.content().length() > 500
            || memory.observedAt() == null
            || invalidValidityRange(
                toOffset(memory.validFrom(), zoneId), toOffset(memory.validTo(), zoneId))) {
          throw new IllegalArgumentException("비교 장기기억 계약이 유효하지 않습니다.");
        }
      }
      AiMemoryResolutionRequest.Candidate resolution =
          new AiMemoryResolutionRequest.Candidate(
              resolutionCandidate.candidateIndex(),
              resolutionCandidate.content(),
              resolutionCandidate.memoryType(),
              resolutionCandidate.sourceMessageIds(),
              resolutionCandidate.observedAt(),
              comparable.stream().map(memory -> toComparable(memory, zoneId)).toList());
      return new CandidateData(candidateIndex, memory, resolution, comparable);
    }

    private static AiMemoryResolutionRequest.ComparableMemory toComparable(
        ConversationMemoryMatch memory, ZoneId zoneId) {
      return new AiMemoryResolutionRequest.ComparableMemory(
          memory.memoryId(),
          memory.content(),
          toOffset(memory.validFrom(), zoneId),
          toOffset(memory.validTo(), zoneId),
          toOffset(memory.observedAt(), zoneId));
    }

    private static OffsetDateTime toOffset(LocalDateTime value, ZoneId zoneId) {
      return value == null ? null : value.atZone(zoneId).toOffsetDateTime();
    }
  }
}
