// 장기기억 AI 추출과 후보 검증을 실행해 저장 계획을 만든다.

package com.landit.landitbe.feature.memory.planning.service;

import com.landit.landitbe.feature.memory.client.ai.AiMemoryClient;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryGenerationRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 장기기억 생성의 외부 호출과 검증 규칙을 소유한다. */
@Service
@RequiredArgsConstructor
public class ConversationMemoryPlanningService {
  private final AiMemoryClient aiClient;
  private final FreeTalkMemoryCandidateMapper candidateMapper;
  private final FreeTalkMemoryResolutionService resolutionService;

  /**
   * 대화에서 기억 후보를 추출하고 저장 가능한 계획으로 검증한다.
   *
   * @param request 생성에 필요한 사용자와 대화 정보
   * @return 검증된 기억 저장 계획
   * @throws IllegalArgumentException AI 응답이 기억 계약을 위반할 때
   */
  public List<ConversationMemoryResolutionPlan> createPlans(
      ConversationMemoryGenerationRequest request) {
    AiMemoryCandidatesResult extraction = extractMemoryCandidates(request);
    List<FreeTalkMemoryCandidate> candidates = candidateMapper.mapCandidates(request, extraction);
    return resolutionService.plan(request, candidates);
  }

  private AiMemoryCandidatesResult extractMemoryCandidates(
      ConversationMemoryGenerationRequest request) {
    return aiClient.extractMemoryCandidates(
        new AiMemoryCandidatesRequest(
            request.learningSessionId(),
            request.characterId(),
            request.targetLocale(),
            request.baseLocale(),
            request.timezone(),
            request.history()));
  }
}
