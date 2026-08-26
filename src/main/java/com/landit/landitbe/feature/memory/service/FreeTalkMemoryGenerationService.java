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

  private static final int MAX_CANDIDATES = 5;
  private static final int MAX_COMPARABLE_MEMORIES = 3;
  private static final int EMBEDDING_DIMENSION = 1536;
  private static final String EMBEDDING_MODEL = "openai/text-embedding-3-small";

  private final FreeTalkMemoryGenerationContextService contextService;
  private final AiFreeTalkClient aiClient;
  private final ConversationMemoryWriteService writeService;
  private final FreeTalkMemoryCandidateMapper candidateMapper;
  private final FreeTalkMemoryResolutionPlanner resolutionPlanner;

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
      List<FreeTalkMemoryCandidate> candidates = candidateMapper.mapCandidates(context, extraction);
      List<ConversationMemoryResolutionPlan> plans = resolutionPlanner.plan(context, candidates);
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
}
