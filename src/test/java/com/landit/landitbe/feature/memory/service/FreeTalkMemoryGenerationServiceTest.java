// 프리톡 장기기억 생성 오케스트레이션의 계약을 검증한다.

package com.landit.landitbe.feature.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.session.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.session.service.FreeTalkMemoryGenerationContextService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** 프리톡 장기기억 생성 오케스트레이션의 계약을 검증한다. */
class FreeTalkMemoryGenerationServiceTest {

  private static final long LEARNING_SESSION_ID = 101L;
  private static final long USER_PROFILE_ID = 202L;
  private static final long USER_MESSAGE_ID = 303L;
  private static final long AI_MESSAGE_ID = 304L;
  private static final OffsetDateTime USER_OCCURRED_AT =
      OffsetDateTime.of(2026, 8, 26, 10, 0, 0, 0, ZoneOffset.ofHours(9));
  private static final OffsetDateTime CANDIDATE_VALID_FROM =
      OffsetDateTime.of(2026, 8, 25, 10, 0, 0, 0, ZoneOffset.ofHours(9));
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-08-26T02:00:00Z"), ZoneOffset.ofHours(9));

  private AiFreeTalkClient aiClient;
  private ConversationMemorySearchRepository searchRepository;
  private ConversationMemoryWriteService writeService;
  private FreeTalkMemoryGenerationContextService contextService;
  private FreeTalkMemoryGenerationService generationService;
  private final JsonMapper jsonMapper =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

  @BeforeEach
  void setUp() {
    aiClient = Mockito.mock(AiFreeTalkClient.class);
    searchRepository = Mockito.mock(ConversationMemorySearchRepository.class);
    writeService = Mockito.mock(ConversationMemoryWriteService.class);
    contextService = Mockito.mock(FreeTalkMemoryGenerationContextService.class);
    generationService =
        new FreeTalkMemoryGenerationService(
            contextService,
            aiClient,
            writeService,
            new FreeTalkMemoryCandidateMapper(CLOCK),
            new FreeTalkMemoryResolutionService(aiClient, searchRepository, CLOCK));
    when(contextService.claim(LEARNING_SESSION_ID)).thenReturn(context());
    when(writeService.persistIfSnapshotCurrent(anyLong(), anyLong(), any()))
        .thenReturn(ConversationMemoryWriteService.PersistenceResult.STORED);
  }

  @Test
  void completesEmptyCandidatesWithoutResolutionOrMemoryPlans() {
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of()));

    generationService.generate(LEARNING_SESSION_ID);

    ArgumentCaptor<List<ConversationMemoryResolutionPlan>> plans =
        ArgumentCaptor.forClass(List.class);
    verify(writeService)
        .persistIfSnapshotCurrent(eq(LEARNING_SESSION_ID), eq(USER_PROFILE_ID), plans.capture());
    assertThat(plans.getValue()).isEmpty();
    verify(aiClient, never()).resolveMemory(any());
    verify(contextService, never()).fail(anyLong());
  }

  @Test
  void locallyAddsSingleCandidateWhenComparableSearchIsEmpty() {
    AiMemoryCandidatesResult.Candidate candidate = candidate(0, USER_MESSAGE_ID, "user fact");
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of(candidate)));
    when(searchRepository.searchActiveComparable(
            any(), eq(USER_PROFILE_ID), eq("chloe"), eq(ConversationMemoryType.EVENT), eq(3)))
        .thenReturn(List.of());

    generationService.generate(LEARNING_SESSION_ID);

    ArgumentCaptor<List<ConversationMemoryResolutionPlan>> plans =
        ArgumentCaptor.forClass(List.class);
    verify(writeService)
        .persistIfSnapshotCurrent(eq(LEARNING_SESSION_ID), eq(USER_PROFILE_ID), plans.capture());
    assertThat(plans.getValue())
        .singleElement()
        .satisfies(
            plan -> {
              assertThat(plan.operation()).isEqualTo(AiMemoryOperation.ADD);
              assertThat(plan.sourceMessageIds()).containsExactly(USER_MESSAGE_ID);
              assertThat(plan.snapshotMemoryIds()).isEmpty();
              assertThat(plan.memory().observedAt()).isEqualTo(USER_OCCURRED_AT.toLocalDateTime());
              assertThat(plan.memory().characterId()).isEqualTo("chloe");
            });
    verify(aiClient, never()).resolveMemory(any());
  }

  @Test
  void resolvesEveryCandidateWhenThereAreMultipleCandidatesEvenWithoutComparables() {
    AiMemoryCandidatesResult.Candidate first = candidate(0, USER_MESSAGE_ID, "first fact");
    AiMemoryCandidatesResult.Candidate second = candidate(1, USER_MESSAGE_ID, "second fact");
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of(first, second)));
    when(searchRepository.searchActiveComparable(
            any(), eq(USER_PROFILE_ID), eq("chloe"), any(), eq(3)))
        .thenReturn(List.of());
    when(aiClient.resolveMemory(any()))
        .thenReturn(
            new AiMemoryResolutionResult(
                List.of(
                    new AiMemoryResolutionResult.Resolution(0, AiMemoryOperation.ADD, List.of()),
                    new AiMemoryResolutionResult.Resolution(
                        1, AiMemoryOperation.IGNORE, List.of()))));

    generationService.generate(LEARNING_SESSION_ID);

    ArgumentCaptor<AiMemoryResolutionRequest> request =
        ArgumentCaptor.forClass(AiMemoryResolutionRequest.class);
    verify(aiClient).resolveMemory(request.capture());
    assertThat(request.getValue().candidates()).hasSize(2);
    assertThat(request.getValue().candidates().getFirst().sourceMessages())
        .containsExactlyElementsOf(
            context().history().stream()
                .filter(message -> message.messageId().equals(USER_MESSAGE_ID))
                .toList());
    verify(writeService)
        .persistIfSnapshotCurrent(eq(LEARNING_SESSION_ID), eq(USER_PROFILE_ID), any());
  }

  @Test
  void rejectsCandidateWithNonUserSourceBeforeWriting() {
    AiMemoryCandidatesResult.Candidate candidate = candidate(0, AI_MESSAGE_ID, "invalid source");
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of(candidate)));

    generationService.generate(LEARNING_SESSION_ID);

    verify(writeService, never()).persistIfSnapshotCurrent(anyLong(), anyLong(), any());
    verify(contextService).fail(LEARNING_SESSION_ID);
  }

  @Test
  void rejectsCandidateWithInvalidEmbeddingModelBeforeWriting() {
    AiMemoryCandidatesResult.Candidate candidate =
        new AiMemoryCandidatesResult.Candidate(
            0,
            ConversationMemoryType.EVENT,
            "invalid model",
            "KR",
            List.of(USER_MESSAGE_ID),
            0.8,
            CANDIDATE_VALID_FROM,
            null,
            "other/model",
            embedding());
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of(candidate)));

    generationService.generate(LEARNING_SESSION_ID);

    verify(searchRepository, never())
        .searchActiveComparable(any(), anyLong(), any(), any(), anyInt());
    verify(writeService, never()).persistIfSnapshotCurrent(anyLong(), anyLong(), any());
    verify(contextService).fail(LEARNING_SESSION_ID);
  }

  @Test
  void failsClosedWhenCandidateIndexIsMissingFromMappedResponse() throws Exception {
    assertMissingCandidateFieldFails("candidateIndex");
  }

  @Test
  void failsClosedWhenConfidenceIsMissingFromMappedResponse() throws Exception {
    assertMissingCandidateFieldFails("confidence");
  }

  @Test
  void failsClosedAfterStaleWriteWithoutRetrying() {
    AiMemoryCandidatesResult.Candidate candidate = candidate(0, USER_MESSAGE_ID, "updated fact");
    ConversationMemoryMatch comparable = comparable(909L);
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of(candidate)));
    when(searchRepository.searchActiveComparable(
            any(), eq(USER_PROFILE_ID), eq("chloe"), eq(ConversationMemoryType.EVENT), eq(3)))
        .thenReturn(List.of(comparable));
    when(aiClient.resolveMemory(any()))
        .thenReturn(
            new AiMemoryResolutionResult(
                List.of(
                    new AiMemoryResolutionResult.Resolution(
                        0, AiMemoryOperation.SUPERSEDE, List.of(909L)))));
    when(writeService.persistIfSnapshotCurrent(anyLong(), anyLong(), any()))
        .thenReturn(ConversationMemoryWriteService.PersistenceResult.STALE);

    generationService.generate(LEARNING_SESSION_ID);

    verify(searchRepository)
        .searchActiveComparable(
            any(), eq(USER_PROFILE_ID), eq("chloe"), eq(ConversationMemoryType.EVENT), eq(3));
    verify(aiClient).resolveMemory(any());
    verify(writeService)
        .persistIfSnapshotCurrent(eq(LEARNING_SESSION_ID), eq(USER_PROFILE_ID), any());
    verify(contextService).fail(LEARNING_SESSION_ID);
  }

  @Test
  void doesNotRunWhenAnotherWorkerAlreadyClaimedTheJob() {
    when(contextService.claim(LEARNING_SESSION_ID)).thenReturn(null);

    generationService.generate(LEARNING_SESSION_ID);

    verify(aiClient, never()).extractMemoryCandidates(any());
    verify(writeService, never()).persistIfSnapshotCurrent(anyLong(), anyLong(), any());
  }

  private FreeTalkMemoryGenerationContextService.GenerationContext context() {
    return new FreeTalkMemoryGenerationContextService.GenerationContext(
        LEARNING_SESSION_ID,
        USER_PROFILE_ID,
        "chloe",
        "EN",
        "KR",
        "Asia/Seoul",
        List.of(
            new AiConversationHistoryMessage(
                USER_MESSAGE_ID, 1, "USER", "hello", null, USER_OCCURRED_AT),
            new AiConversationHistoryMessage(
                AI_MESSAGE_ID, 1, "AI", "hi", null, USER_OCCURRED_AT.plusMinutes(1))));
  }

  private AiMemoryCandidatesResult.Candidate candidate(
      int index, long sourceMessageId, String content) {
    return new AiMemoryCandidatesResult.Candidate(
        index,
        ConversationMemoryType.EVENT,
        content,
        "KR",
        List.of(sourceMessageId),
        0.8,
        CANDIDATE_VALID_FROM,
        null,
        "openai/text-embedding-3-small",
        embedding());
  }

  private ConversationMemoryMatch comparable(long memoryId) {
    LocalDateTime now = LocalDateTime.of(2026, 8, 24, 10, 0);
    return new ConversationMemoryMatch(
        memoryId, ConversationMemoryType.EVENT, "old fact", now, null, now, 0.1);
  }

  private void assertMissingCandidateFieldFails(String fieldName) throws Exception {
    when(aiClient.extractMemoryCandidates(any())).thenReturn(mappedCandidateWithout(fieldName));
    when(searchRepository.searchActiveComparable(
            any(), eq(USER_PROFILE_ID), eq("chloe"), eq(ConversationMemoryType.EVENT), eq(3)))
        .thenReturn(List.of());

    generationService.generate(LEARNING_SESSION_ID);

    verify(writeService, never()).persistIfSnapshotCurrent(anyLong(), anyLong(), any());
    verify(contextService).fail(LEARNING_SESSION_ID);
  }

  private AiMemoryCandidatesResult mappedCandidateWithout(String fieldName) throws Exception {
    AiMemoryCandidatesResult valid =
        new AiMemoryCandidatesResult(
            "extractor-v1", List.of(candidate(0, USER_MESSAGE_ID, "user fact")));
    JsonNode root = jsonMapper.valueToTree(valid);
    ((ObjectNode) root.get("candidates").get(0)).remove(fieldName);
    return jsonMapper.treeToValue(root, AiMemoryCandidatesResult.class);
  }

  private static List<Float> embedding() {
    List<Float> embedding = new ArrayList<>(Collections.nCopies(1536, 0.0f));
    embedding.set(0, 1.0f);
    return embedding;
  }
}
