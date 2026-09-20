// 완료 프리톡의 장기기억 후보 생성과 저장을 조율한다.

package com.landit.landitbe.feature.learning.freetalk.memory.service;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryGenerationRequest;
import com.landit.landitbe.feature.memory.planning.service.ConversationMemoryPlanningService;
import com.landit.landitbe.feature.memory.service.ConversationMemoryWriteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 완료 프리톡의 장기기억 후보 생성과 저장을 조율한다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkMemoryGenerationService {

  private final FreeTalkMemoryGenerationContextService contextService;
  private final ConversationMemoryPlanningService planningService;

  /**
   * 완료된 프리톡의 장기기억 생성을 선점하고 실행한다.
   *
   * @param learningSessionId 완료된 학습 세션 ID
   */
  public void generate(long learningSessionId) {
    ConversationMemoryGenerationRequest request = claimContextOrFail(learningSessionId);
    if (request == null) {
      return;
    }
    generate(request);
  }

  /** 장기기억 생성 문맥으로 후보를 판정하고 저장한다. */
  private void generate(ConversationMemoryGenerationRequest request) {
    try {
      List<ConversationMemoryResolutionPlan> plans = planningService.createPlans(request);
      if (contextService.persistAndComplete(request, plans)
          == ConversationMemoryWriteService.PersistenceResult.STALE) {
        throw new IllegalStateException("장기기억 비교 snapshot이 변경됐습니다.");
      }
    } catch (RuntimeException exception) {
      failSafely(request.learningSessionId(), exception);
    }
  }

  private ConversationMemoryGenerationRequest claimContextOrFail(long learningSessionId) {
    try {
      return contextService.claim(learningSessionId);
    } catch (RuntimeException exception) {
      failSafely(learningSessionId, exception);
      return null;
    }
  }

  /**
   * 작업 제출 실패 등으로 실행되지 못한 기억 생성을 실패 상태로 전환한다.
   *
   * @param learningSessionId 학습 세션 ID
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
