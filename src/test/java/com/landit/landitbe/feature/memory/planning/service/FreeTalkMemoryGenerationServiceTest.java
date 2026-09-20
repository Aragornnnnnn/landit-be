// 프리톡 장기기억 생성 오케스트레이션의 계약을 검증한다.

package com.landit.landitbe.feature.memory.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.freetalk.memory.service.FreeTalkMemoryGenerationContextService;
import com.landit.landitbe.feature.learning.freetalk.memory.service.FreeTalkMemoryGenerationService;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.client.ai.AiMemoryClient;
import com.landit.landitbe.feature.memory.client.ai.ConversationMemoryHistoryMessage;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryFollowUpContext;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryGenerationRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryRepository;
import com.landit.landitbe.feature.memory.retrieval.dto.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.retrieval.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.memory.service.ConversationMemoryWriteService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

  private AiMemoryClient aiClient;
  private ConversationMemorySearchRepository searchRepository;
  private FreeTalkMemoryGenerationContextService contextService;
  private ConversationMemoryRepository memoryRepository;
  private FreeTalkMemoryGenerationService generationService;
  private final JsonMapper jsonMapper =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

  @BeforeEach
  void setUp() {
    aiClient = Mockito.mock(AiMemoryClient.class);
    searchRepository = Mockito.mock(ConversationMemorySearchRepository.class);
    contextService = Mockito.mock(FreeTalkMemoryGenerationContextService.class);
    memoryRepository = Mockito.mock(ConversationMemoryRepository.class);
    generationService =
        new FreeTalkMemoryGenerationService(
            contextService,
            new ConversationMemoryPlanningService(
                aiClient,
                new FreeTalkMemoryCandidateMapper(CLOCK),
                new FreeTalkMemoryResolutionService(aiClient, searchRepository, CLOCK),
                memoryRepository,
                new ConversationMemoryFollowUpResolver()));
    when(contextService.claim(LEARNING_SESSION_ID)).thenReturn(context());
    when(contextService.persistAndComplete(any(), any()))
        .thenReturn(ConversationMemoryWriteService.PersistenceResult.STORED);
  }

  @DisplayName("기억 후보 추출 요청에 기존 기억과 이미 질문에 쓴 기억, 세션 종료 방식을 함께 보낸다.")
  @Test
  void sendsExistingMemoriesAndFollowUpContextWithCandidateRequest() {
    List<AiFreeTalkMemoryContext> existing =
        List.of(new AiFreeTalkMemoryContext(42L, ConversationMemoryType.EVENT, "다음 주에 면접이 있다."));
    when(memoryRepository.findRecentActiveContexts(USER_PROFILE_ID, "chloe", 20))
        .thenReturn(existing);
    ConversationMemoryGenerationRequest base = context();
    when(contextService.claim(LEARNING_SESSION_ID))
        .thenReturn(
            new ConversationMemoryGenerationRequest(
                base.learningSessionId(),
                base.userProfileId(),
                base.characterId(),
                base.targetLocale(),
                base.baseLocale(),
                base.timezone(),
                base.history(),
                new ConversationMemoryFollowUpContext(List.of(7L), "USER_CONFIRMED")));
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of()));

    generationService.generate(LEARNING_SESSION_ID);

    ArgumentCaptor<AiMemoryCandidatesRequest> request =
        ArgumentCaptor.forClass(AiMemoryCandidatesRequest.class);
    verify(aiClient).extractMemoryCandidates(request.capture());
    assertThat(request.getValue().existingMemories()).isEqualTo(existing);
    assertThat(request.getValue().askedMemoryIds()).containsExactly(7L);
    assertThat(request.getValue().sessionEndedBy()).isEqualTo("USER_CONFIRMED");
  }

  @DisplayName("기억 후보가 없으면 충돌 해결이나 저장 계획 없이 작업을 완료한다.")
  @Test
  void completesEmptyCandidatesWithoutResolutionOrMemoryPlans() {
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of()));

    generationService.generate(LEARNING_SESSION_ID);

    ArgumentCaptor<List<ConversationMemoryResolutionPlan>> plans =
        ArgumentCaptor.forClass(List.class);
    verify(contextService)
        .persistAndComplete(any(ConversationMemoryGenerationRequest.class), plans.capture());
    assertThat(plans.getValue()).isEmpty();
    verify(aiClient, never()).resolveMemory(any());
    verify(contextService, never()).fail(anyLong());
  }

  @DisplayName("후보가 하나이고 비교 기억이 없으면 로컬에서 추가 계획을 만든다.")
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
    verify(contextService)
        .persistAndComplete(any(ConversationMemoryGenerationRequest.class), plans.capture());
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

  @DisplayName("후보가 여러 개면 비교 기억이 없어도 모든 후보의 충돌을 해결한다.")
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
    verify(contextService)
        .persistAndComplete(any(ConversationMemoryGenerationRequest.class), any());
  }

  @DisplayName("사용자 발화가 아닌 출처의 기억 후보는 저장 전에 거부한다.")
  @Test
  void rejectsCandidateWithNonUserSourceBeforeWriting() {
    AiMemoryCandidatesResult.Candidate candidate = candidate(0, AI_MESSAGE_ID, "invalid source");
    when(aiClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of(candidate)));

    generationService.generate(LEARNING_SESSION_ID);

    verify(contextService, never()).persistAndComplete(any(), any());
    verify(contextService).fail(LEARNING_SESSION_ID);
  }

  @DisplayName("임베딩 모델이 잘못된 기억 후보는 저장 전에 거부한다.")
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
    verify(contextService, never()).persistAndComplete(any(), any());
    verify(contextService).fail(LEARNING_SESSION_ID);
  }

  @DisplayName("AI 응답에 후보 인덱스가 없으면 기억 생성을 실패로 처리한다.")
  @Test
  void failsClosedWhenCandidateIndexIsMissingFromMappedResponse() throws Exception {
    assertMissingCandidateFieldFails("candidateIndex");
  }

  @DisplayName("AI 응답에 신뢰도가 없으면 기억 생성을 실패로 처리한다.")
  @Test
  void failsClosedWhenConfidenceIsMissingFromMappedResponse() throws Exception {
    assertMissingCandidateFieldFails("confidence");
  }

  @DisplayName("기억 저장 대상이 변경되어 있으면 재시도 없이 실패로 처리한다.")
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
    when(contextService.persistAndComplete(any(), any()))
        .thenReturn(ConversationMemoryWriteService.PersistenceResult.STALE);

    generationService.generate(LEARNING_SESSION_ID);

    verify(searchRepository)
        .searchActiveComparable(
            any(), eq(USER_PROFILE_ID), eq("chloe"), eq(ConversationMemoryType.EVENT), eq(3));
    verify(aiClient).resolveMemory(any());
    verify(contextService)
        .persistAndComplete(any(ConversationMemoryGenerationRequest.class), any());
    verify(contextService).fail(LEARNING_SESSION_ID);
  }

  @DisplayName("다른 Worker가 선점한 기억 생성 작업은 실행하지 않는다.")
  @Test
  void doesNotRunWhenAnotherWorkerAlreadyClaimedTheJob() {
    when(contextService.claim(LEARNING_SESSION_ID)).thenReturn(null);

    generationService.generate(LEARNING_SESSION_ID);

    verify(aiClient, never()).extractMemoryCandidates(any());
    verify(contextService, never()).persistAndComplete(any(), any());
  }

  private ConversationMemoryGenerationRequest context() {
    return new ConversationMemoryGenerationRequest(
        LEARNING_SESSION_ID,
        USER_PROFILE_ID,
        "chloe",
        "EN",
        "KR",
        "Asia/Seoul",
        List.of(
            new ConversationMemoryHistoryMessage(
                USER_MESSAGE_ID, 1, "USER", "hello", null, USER_OCCURRED_AT),
            new ConversationMemoryHistoryMessage(
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

    verify(contextService, never()).persistAndComplete(any(), any());
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
