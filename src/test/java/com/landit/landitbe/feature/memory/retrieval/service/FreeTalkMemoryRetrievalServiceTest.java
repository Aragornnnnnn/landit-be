// 프리톡 장기기억 검색 서비스의 fail-open과 사용 trace 연결을 검증한다.

package com.landit.landitbe.feature.memory.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.memory.MemoryProperties;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.client.ai.AiMemoryClient;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingResult;
import com.landit.landitbe.feature.memory.retrieval.domain.MemoryRetrievalStage;
import com.landit.landitbe.feature.memory.retrieval.dto.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.retrieval.dto.MemoryRetrievalRequest;
import com.landit.landitbe.feature.memory.retrieval.dto.MemoryRetrievalResult;
import com.landit.landitbe.feature.memory.retrieval.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.memory.retrieval.repository.FreeTalkMemoryRetrievalTraceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 프리톡 세션 시작 장기기억 검색과 fail-open 경계를 검증한다. */
class FreeTalkMemoryRetrievalServiceTest {

  private AiMemoryClient aiClient;
  private ConversationMemorySearchRepository searchRepository;
  private FreeTalkMemoryRetrievalTraceRepository traceRepository;
  private SimpleMeterRegistry meterRegistry;
  private FreeTalkMemoryRetrievalService service;

  @BeforeEach
  void setUp() {
    aiClient = Mockito.mock(AiMemoryClient.class);
    searchRepository = Mockito.mock(ConversationMemorySearchRepository.class);
    traceRepository = Mockito.mock(FreeTalkMemoryRetrievalTraceRepository.class);
    meterRegistry = new SimpleMeterRegistry();
    service =
        new FreeTalkMemoryRetrievalService(
            aiClient,
            searchRepository,
            traceRepository,
            new MemoryProperties(false, true),
            meterRegistry);
  }

  @DisplayName("기억 사용이 켜져 있으면 허용 범위에서 상위 기억 3개를 반환한다.")
  @Test
  void returnsTopThreeScopedMemoriesWhenUseIsEnabled() {
    when(traceRepository.claim(10L, MemoryRetrievalStage.OPENING, "memory-retrieval-v2"))
        .thenReturn(true);
    when(aiClient.embedMemoryQuery(any(AiMemoryQueryEmbeddingRequest.class)))
        .thenReturn(new AiMemoryQueryEmbeddingResult("openai/text-embedding-3-small", embedding()));
    when(searchRepository.searchActive(20L, "chloe", embedding(), 3))
        .thenReturn(List.of(match(1L, 0.1), match(2L, 0.2), match(3L, 0.3), match(4L, 0.4)));

    MemoryRetrievalResult result =
        service.retrieve(
            new MemoryRetrievalRequest(
                10L, 20L, "chloe", MemoryRetrievalStage.OPENING, "weekend plans"));

    assertThat(result.contexts())
        .extracting(AiFreeTalkMemoryContext::memoryId)
        .containsExactly(1L, 2L, 3L);
    assertThat(result.contexts().getFirst().validFrom())
        .isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
    assertThat(result.contexts().getFirst().validTo())
        .isEqualTo(LocalDateTime.of(2026, 7, 31, 23, 59));
    assertThat(result.contexts().getFirst().observedAt())
        .isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 30));
    verify(traceRepository)
        .saveCandidates(
            eq(10L), eq(MemoryRetrievalStage.OPENING), any(), eq("memory-retrieval-v2"));

    service.recordUsage(result, List.of(2L), 99L);

    verify(traceRepository).recordUsage(10L, MemoryRetrievalStage.OPENING, List.of(2L), 99L);
  }

  @DisplayName("최소 코사인 유사도보다 낮은 기억은 검색 결과에서 제외한다.")
  @Test
  void excludesMemoriesBelowMinimumCosineSimilarity() {
    when(traceRepository.claim(10L, MemoryRetrievalStage.FIRST_USER_TURN, "memory-retrieval-v2"))
        .thenReturn(true);
    when(aiClient.embedMemoryQuery(any(AiMemoryQueryEmbeddingRequest.class)))
        .thenReturn(new AiMemoryQueryEmbeddingResult("openai/text-embedding-3-small", embedding()));
    when(searchRepository.searchActive(20L, "chloe", embedding(), 3))
        .thenReturn(List.of(match(1L, 0.79), match(2L, 0.81), match(3L, 0.95)));

    MemoryRetrievalResult result =
        service.retrieve(
            new MemoryRetrievalRequest(
                10L, 20L, "chloe", MemoryRetrievalStage.FIRST_USER_TURN, "Saturday routine"));

    assertThat(result.contexts()).extracting(AiFreeTalkMemoryContext::memoryId).containsExactly(1L);
  }

  @DisplayName("기억 검색이 실패하면 빈 기억 문맥을 반환한다.")
  @Test
  void returnsEmptyContextWhenMemorySearchFails() {
    when(traceRepository.claim(10L, MemoryRetrievalStage.FIRST_USER_TURN, "memory-retrieval-v2"))
        .thenReturn(true);
    when(aiClient.embedMemoryQuery(any())).thenThrow(new RuntimeException("AI unavailable"));

    MemoryRetrievalResult result =
        service.retrieve(
            new MemoryRetrievalRequest(
                10L, 20L, "chloe", MemoryRetrievalStage.FIRST_USER_TURN, "I like hiking"));

    assertThat(result.contexts()).isEmpty();
    verify(searchRepository, never()).searchActive(anyLong(), anyString(), any(), anyInt());
    assertThat(
            meterRegistry
                .get("landit.memory.fallback")
                .tag("stage", MemoryRetrievalStage.FIRST_USER_TURN.name())
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @DisplayName("세션의 검색 이력이 이미 있으면 기억을 다시 검색하지 않는다.")
  @Test
  void doesNotSearchTwiceWhenSessionTraceAlreadyExists() {
    when(traceRepository.claim(10L, MemoryRetrievalStage.OPENING, "memory-retrieval-v2"))
        .thenReturn(false);

    MemoryRetrievalResult result =
        service.retrieve(
            new MemoryRetrievalRequest(
                10L, 20L, "chloe", MemoryRetrievalStage.OPENING, "weekend plans"));

    assertThat(result.contexts()).isEmpty();
    verify(aiClient, never()).embedMemoryQuery(any());
    verify(searchRepository, never()).searchActive(anyLong(), anyString(), any(), anyInt());
  }

  @DisplayName("세션에서 이미 검색한 기억은 임베딩 요청이나 새 검색 없이 최대 3개를 다시 돌려준다.")
  @Test
  void returnsAlreadyRetrievedContextsWithoutSearchingAgain() {
    List<AiFreeTalkMemoryContext> retrieved =
        List.of(new AiFreeTalkMemoryContext(42L, ConversationMemoryType.PROFILE, "집 앞 헬스장에 다닌다."));
    when(traceRepository.findRetrievedContexts(10L, 20L, 3)).thenReturn(retrieved);

    assertThat(service.retrievedContexts(10L, 20L)).isEqualTo(retrieved);
    verify(aiClient, never()).embedMemoryQuery(any());
    verify(searchRepository, never()).searchActive(anyLong(), anyString(), any(), anyInt());
  }

  @DisplayName("검색한 기억을 다시 읽다 실패하면 대화를 막지 않고 빈 문맥으로 전환한다.")
  @Test
  void returnsEmptyRetrievedContextsWhenLookupFails() {
    when(traceRepository.findRetrievedContexts(10L, 20L, 3))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThat(service.retrievedContexts(10L, 20L)).isEmpty();
  }

  @DisplayName("기억 사용이 꺼져 있으면 검색한 기억을 읽지 않고 빈 문맥을 돌려준다.")
  @Test
  void returnsEmptyRetrievedContextsWhenMemoryUseIsDisabled() {
    FreeTalkMemoryRetrievalService disabledService =
        new FreeTalkMemoryRetrievalService(
            aiClient,
            searchRepository,
            traceRepository,
            new MemoryProperties(false, false),
            meterRegistry);

    assertThat(disabledService.retrievedContexts(10L, 20L)).isEmpty();
    verify(traceRepository, never()).findRetrievedContexts(anyLong(), anyLong(), anyInt());
  }

  private ConversationMemoryMatch match(long memoryId, double distance) {
    return new ConversationMemoryMatch(
        memoryId,
        ConversationMemoryType.EVENT,
        "memory " + memoryId,
        LocalDateTime.of(2026, 7, 1, 9, 0),
        LocalDateTime.of(2026, 7, 31, 23, 59),
        LocalDateTime.of(2026, 8, 1, 10, 30),
        distance);
  }

  private List<Float> embedding() {
    return Collections.nCopies(1536, 0.0f);
  }
}
