// 완료 프리톡의 장기기억 후보를 비교하고 원자 저장까지 오케스트레이션한다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.session.service.FreeTalkMemoryGenerationContextService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 완료 프리톡의 장기기억 후보를 비교하고 원자 저장까지 오케스트레이션한다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkMemoryGenerationService {

  private final FreeTalkMemoryGenerationContextService contextService;
  private final AiFreeTalkClient aiClient;
  private final ConversationMemoryWriteService writeService;
  private final FreeTalkMemoryCandidateMapper candidateMapper;
  private final FreeTalkMemoryResolutionService resolutionService;

  /**
   * 완료된 프리톡의 대화 이력에서 장기기억을 생성하고 저장한다. 생성 과정의 실패는 실패 상태로 기록하고 프리톡 흐름으로 전파하지 않는다.
   *
   * @param learningSessionId 장기기억을 생성할 완료 프리톡 세션 ID
   */
  public void generate(long learningSessionId) {
    FreeTalkMemoryGenerationContextService.GenerationContext context =
        claimContextOrFail(learningSessionId);
    if (context == null) {
      return;
    }

    try {
      persistGeneratedMemories(learningSessionId, context);
    } catch (RuntimeException exception) {
      failSafely(learningSessionId, exception);
    }
  }

  private FreeTalkMemoryGenerationContextService.GenerationContext claimContextOrFail(
      long learningSessionId) {
    try {
      return contextService.claim(learningSessionId);
    } catch (RuntimeException exception) {
      failSafely(learningSessionId, exception);
      return null;
    }
  }

  private void persistGeneratedMemories(
      long learningSessionId, FreeTalkMemoryGenerationContextService.GenerationContext context) {
    List<ConversationMemoryResolutionPlan> plans = createResolutionPlans(context);
    if (writeService.persistIfSnapshotCurrent(learningSessionId, context.userProfileId(), plans)
        == ConversationMemoryWriteService.PersistenceResult.STALE) {
      throw new IllegalStateException("장기기억 비교 snapshot이 변경됐습니다.");
    }
  }

  private List<ConversationMemoryResolutionPlan> createResolutionPlans(
      FreeTalkMemoryGenerationContextService.GenerationContext context) {
    AiMemoryCandidatesResult extraction = extractMemoryCandidates(context);
    List<FreeTalkMemoryCandidate> candidates = candidateMapper.mapCandidates(context, extraction);
    return resolutionService.plan(context, candidates);
  }

  private AiMemoryCandidatesResult extractMemoryCandidates(
      FreeTalkMemoryGenerationContextService.GenerationContext context) {
    return aiClient.extractMemoryCandidates(
        new AiMemoryCandidatesRequest(
            context.learningSessionId(),
            context.characterId(),
            context.targetLocale(),
            context.baseLocale(),
            context.timezone(),
            context.history()));
  }

  /**
   * 작업 제출 실패 등으로 실행되지 못한 장기기억 생성 작업을 조건부 실패 상태로 전환한다.
   *
   * @param learningSessionId 실패 상태로 전환할 프리톡 세션 ID
   */
  public void markFailed(long learningSessionId) {
    failSafely(learningSessionId, null);
  }

  /** 실패 상태 전환 자체의 예외가 후속 작업 실패 처리를 막지 않도록 삼킨다. */
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
}
