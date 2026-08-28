// 프리톡 장기기억 검색 서비스의 fail-open과 사용 trace 연결을 검증한다.

package com.landit.landitbe.feature.memory.service;

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
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.memory.repository.FreeTalkMemoryRetrievalTraceRepository;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.session.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryQueryEmbeddingResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 프리톡 세션 시작 장기기억 검색과 fail-open 경계를 검증한다. */
class FreeTalkMemoryRetrievalServiceTest {

  private AiFreeTalkClient aiClient;
  private ConversationMemorySearchRepository searchRepository;
  private FreeTalkMemoryRetrievalTraceRepository traceRepository;
  private SimpleMeterRegistry meterRegistry;
  private FreeTalkMemoryRetrievalService service;

  @BeforeEach
  void setUp() {
    aiClient = Mockito.mock(AiFreeTalkClient.class);
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

  @Test
  void returnsTopThreeScopedMemoriesWhenUseIsEnabled() {
    when(traceRepository.claim(10L, MemoryRetrievalStage.OPENING, "memory-retrieval-v1"))
        .thenReturn(true);
    when(aiClient.embedMemoryQuery(any(AiMemoryQueryEmbeddingRequest.class)))
        .thenReturn(new AiMemoryQueryEmbeddingResult("openai/text-embedding-3-small", embedding()));
    when(searchRepository.searchActive(20L, "chloe", embedding(), 3))
        .thenReturn(List.of(match(1L, 0.1), match(2L, 0.2), match(3L, 0.3), match(4L, 0.4)));

    FreeTalkMemoryRetrievalService.RetrievalResult result =
        service.retrieve(
            new FreeTalkMemoryRetrievalService.RetrievalRequest(
                10L, 20L, "chloe", MemoryRetrievalStage.OPENING, "weekend plans"));

    assertThat(result.contexts())
        .extracting(AiFreeTalkMemoryContext::memoryId)
        .containsExactly(1L, 2L, 3L);
    verify(traceRepository)
        .saveCandidates(
            eq(10L), eq(MemoryRetrievalStage.OPENING), any(), eq("memory-retrieval-v1"));

    service.recordUsage(result, List.of(2L), 99L);

    verify(traceRepository).recordUsage(10L, MemoryRetrievalStage.OPENING, List.of(2L), 99L);
  }

  @Test
  void returnsEmptyContextWhenMemorySearchFails() {
    when(traceRepository.claim(10L, MemoryRetrievalStage.FIRST_USER_TURN, "memory-retrieval-v1"))
        .thenReturn(true);
    when(aiClient.embedMemoryQuery(any())).thenThrow(new RuntimeException("AI unavailable"));

    FreeTalkMemoryRetrievalService.RetrievalResult result =
        service.retrieve(
            new FreeTalkMemoryRetrievalService.RetrievalRequest(
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

  @Test
  void doesNotSearchTwiceWhenSessionTraceAlreadyExists() {
    when(traceRepository.claim(10L, MemoryRetrievalStage.OPENING, "memory-retrieval-v1"))
        .thenReturn(false);

    FreeTalkMemoryRetrievalService.RetrievalResult result =
        service.retrieve(
            new FreeTalkMemoryRetrievalService.RetrievalRequest(
                10L, 20L, "chloe", MemoryRetrievalStage.OPENING, "weekend plans"));

    assertThat(result.contexts()).isEmpty();
    verify(aiClient, never()).embedMemoryQuery(any());
    verify(searchRepository, never()).searchActive(anyLong(), anyString(), any(), anyInt());
  }

  private ConversationMemoryMatch match(long memoryId, double distance) {
    return new ConversationMemoryMatch(
        memoryId, ConversationMemoryType.EVENT, "memory " + memoryId, null, null, null, distance);
  }

  private List<Float> embedding() {
    return Collections.nCopies(1536, 0.0f);
  }
}
