// 프리톡 세션 시작 장기기억 검색과 사용 trace를 fail-open으로 조율한다.

package com.landit.landitbe.feature.memory.retrieval.service;

import com.landit.landitbe.config.memory.MemoryProperties;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.client.ai.AiMemoryClient;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingResult;
import com.landit.landitbe.feature.memory.retrieval.dto.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.retrieval.dto.MemoryRetrievalRequest;
import com.landit.landitbe.feature.memory.retrieval.dto.MemoryRetrievalResult;
import com.landit.landitbe.feature.memory.retrieval.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.memory.retrieval.repository.FreeTalkMemoryRetrievalTraceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 프리톡 세션 시작에 한 번만 장기기억을 검색하고 사용 결과를 기록한다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkMemoryRetrievalService {

  static final String POLICY_VERSION = "memory-retrieval-v2";
  private static final int MAX_RESULTS = AiFreeTalkMemoryContext.MAX_CONTEXTS;
  private static final double MAX_DISTANCE = 0.8;
  private static final int EMBEDDING_DIMENSION = 1536;
  private static final String EMBEDDING_MODEL = "openai/text-embedding-3-small";

  private final AiMemoryClient aiClient;
  private final ConversationMemorySearchRepository searchRepository;
  private final FreeTalkMemoryRetrievalTraceRepository traceRepository;
  private final MemoryProperties memoryProperties;
  private final MeterRegistry meterRegistry;

  /**
   * 활성화된 경우 세션의 지정 단계에서 장기기억을 한 번 검색한다.
   *
   * @param request 장기기억 검색에 필요한 세션·사용자·질의 정보
   * @return 검색 문맥과 trace 선점 여부. 검색 실패 시 빈 문맥을 반환한다.
   */
  public MemoryRetrievalResult retrieve(MemoryRetrievalRequest request) {
    if (!memoryProperties.useEnabled()) {
      return MemoryRetrievalResult.empty(request.sessionId(), request.stage());
    }
    try {
      return retrieveWhenEnabled(request);
    } catch (RuntimeException exception) {
      meterRegistry.counter("landit.memory.fallback", "stage", request.stage().name()).increment();
      return fallback(request);
    }
  }

  /**
   * 세션 시작 때 이미 검색한 기억을 새 검색 없이 다시 돌려준다.
   *
   * <p>턴 교정처럼 매 턴 기억이 필요한 호출이 임베딩 요청과 지연을 더하지 않게 한다. 조회 실패는 빈 문맥으로 전환한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param userProfileId 세션 소유 사용자 프로필 ID
   * @return 가까운 순으로 최대 3개인 기억 문맥. 기억 사용이 꺼져 있거나 검색된 기억이 없으면 빈 목록
   */
  public List<AiFreeTalkMemoryContext> retrievedContexts(long sessionId, long userProfileId) {
    if (!memoryProperties.useEnabled()) {
      return List.of();
    }
    try {
      return traceRepository.findRetrievedContexts(sessionId, userProfileId, MAX_RESULTS);
    } catch (RuntimeException exception) {
      // 조회 실패가 조용히 묻히면 기억 근거 교정이 통째로 꺼진 것을 알 수 없으므로 원인과 지표를 남긴다.
      meterRegistry.counter("landit.memory.fallback", "stage", "RETRIEVED_CONTEXT").increment();
      log.warn("프리톡 세션의 검색된 장기기억을 읽지 못해 빈 문맥으로 진행합니다. sessionId={}", sessionId, exception);
      return List.of();
    }
  }

  /** 선점한 단계에서 임베딩·검색·후보 trace를 한 흐름으로 처리한다. */
  private MemoryRetrievalResult retrieveWhenEnabled(MemoryRetrievalRequest request) {
    if (!traceRepository.claim(request.sessionId(), request.stage(), POLICY_VERSION)) {
      return MemoryRetrievalResult.empty(request.sessionId(), request.stage());
    }
    AiMemoryQueryEmbeddingResult embeddingResult =
        aiClient.embedMemoryQuery(new AiMemoryQueryEmbeddingRequest(request.query()));
    validateEmbeddingResult(embeddingResult);
    List<ConversationMemoryMatch> matches = searchMatches(request, embeddingResult);
    List<AiFreeTalkMemoryContext> contexts = toContexts(matches);
    traceRepository.saveCandidates(request.sessionId(), request.stage(), matches, POLICY_VERSION);
    return new MemoryRetrievalResult(request.sessionId(), request.stage(), contexts, true);
  }

  /** 검색 저장소 결과를 최대 반환 수로 제한하고 null 응답을 거부한다. */
  private List<ConversationMemoryMatch> searchMatches(
      MemoryRetrievalRequest request, AiMemoryQueryEmbeddingResult embeddingResult) {
    List<ConversationMemoryMatch> matches =
        searchRepository.searchActive(
            request.userProfileId(),
            request.characterId(),
            embeddingResult.embedding(),
            MAX_RESULTS);
    if (matches == null) {
      throw new IllegalArgumentException("장기기억 검색 후보 개수가 유효하지 않습니다.");
    }
    return matches.stream()
        .filter(match -> match.distance() <= MAX_DISTANCE)
        .limit(MAX_RESULTS)
        .toList();
  }

  private static List<AiFreeTalkMemoryContext> toContexts(List<ConversationMemoryMatch> matches) {
    return matches.stream().map(FreeTalkMemoryRetrievalService::toContext).toList();
  }

  /** 검색 실패는 대화 요청을 막지 않고 빈 문맥으로 전환한다. */
  private MemoryRetrievalResult fallback(MemoryRetrievalRequest request) {
    log.warn("프리톡 장기기억 검색을 건너뜁니다. stage={} policyVersion={}", request.stage(), POLICY_VERSION);
    return MemoryRetrievalResult.empty(request.sessionId(), request.stage());
  }

  /**
   * AI가 실제 사용한 기억을 제공 문맥의 부분집합으로 검증해 trace에 기록한다.
   *
   * @param result 검색 단계의 결과와 제공 문맥
   * @param usedMemoryIds AI가 실제 사용했다고 응답한 장기기억 ID 목록
   * @param responseMessageId 장기기억을 사용한 AI 응답 메시지 ID
   */
  public void recordUsage(
      MemoryRetrievalResult result, List<Long> usedMemoryIds, Long responseMessageId) {
    if (!result.claimed()) {
      return;
    }
    List<Long> normalized = normalizeUsedMemoryIds(result.contexts(), usedMemoryIds);
    try {
      traceRepository.recordUsage(
          result.sessionId(), result.stage(), normalized, responseMessageId);
    } catch (RuntimeException exception) {
      log.warn(
          "프리톡 장기기억 사용 trace를 저장하지 못했습니다. stage={} policyVersion={}",
          result.stage(),
          POLICY_VERSION);
    }
  }

  /** AI가 사용했다고 답한 ID는 이번 응답에 제공한 기억의 부분집합일 때만 기록한다. */
  private static List<Long> normalizeUsedMemoryIds(
      List<AiFreeTalkMemoryContext> contexts, List<Long> usedMemoryIds) {
    List<Long> normalized = usedMemoryIds == null ? List.of() : usedMemoryIds;
    Set<Long> contextIds =
        contexts.stream()
            .map(AiFreeTalkMemoryContext::memoryId)
            .collect(java.util.stream.Collectors.toSet());
    if (normalized.stream().anyMatch(id -> id == null || id <= 0)
        || normalized.size() != new HashSet<>(normalized).size()
        || !contextIds.containsAll(normalized)) {
      return List.of();
    }
    return normalized;
  }

  private static AiFreeTalkMemoryContext toContext(ConversationMemoryMatch match) {
    if (match == null
        || match.memoryId() <= 0
        || match.memoryType() == null
        || match.content() == null
        || match.content().isBlank()) {
      throw new IllegalArgumentException("장기기억 검색 후보가 유효하지 않습니다.");
    }
    return new AiFreeTalkMemoryContext(
        match.memoryId(),
        match.memoryType(),
        match.content(),
        match.validFrom(),
        match.validTo(),
        match.observedAt());
  }

  private static void validateEmbeddingResult(AiMemoryQueryEmbeddingResult result) {
    if (result == null
        || !EMBEDDING_MODEL.equals(result.embeddingModel())
        || result.embedding() == null
        || result.embedding().size() != EMBEDDING_DIMENSION
        || result.embedding().stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
      throw new IllegalArgumentException("장기기억 query embedding 계약이 유효하지 않습니다.");
    }
  }
}
