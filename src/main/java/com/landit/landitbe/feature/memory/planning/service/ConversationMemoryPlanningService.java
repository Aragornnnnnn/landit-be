// 장기기억 AI 추출과 후보 검증을 실행해 저장 계획을 만든다.

package com.landit.landitbe.feature.memory.planning.service;

import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.client.ai.AiMemoryClient;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryGenerationRequest;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryPlanningResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryRepository;
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
  private final ConversationMemoryRepository memoryRepository;
  private final ConversationMemoryFollowUpResolver followUpResolver;

  /**
   * AI 후보 추출과 충돌 해결을 실행해 저장 계획을 만들고, 같은 응답의 후속 질문을 계획과 연결한다.
   *
   * @param request 장기기억 생성 문맥
   * @return 검증된 기억 저장 계획과 후속 질문
   */
  public ConversationMemoryPlanningResult createPlans(ConversationMemoryGenerationRequest request) {
    // 후속 질문의 근거로 쓸 수 있도록 이번 세션의 기억을 저장하기 전의 기존 기억을 함께 보낸다.
    // 이미 질문에 쓴 기억은 AI 서버가 어차피 고르지 않으므로, 상한만큼의 자리를 아직 묻지 않은 기억으로 채운다.
    List<AiFreeTalkMemoryContext> existingMemories =
        memoryRepository.findRecentActiveContexts(
            request.userProfileId(),
            request.characterId(),
            request.followUpContext().askedMemoryIds(),
            AiMemoryCandidatesRequest.MAX_EXISTING_MEMORIES);
    AiMemoryCandidatesResult extraction = extractMemoryCandidates(request, existingMemories);
    List<FreeTalkMemoryCandidate> candidates = candidateMapper.mapCandidates(request, extraction);
    List<ConversationMemoryResolutionPlan> plans = resolutionService.plan(request, candidates);
    return new ConversationMemoryPlanningResult(
        plans,
        followUpResolver.resolve(
            request.learningSessionId(),
            extraction.followUpQuestion(),
            existingMemories,
            candidates,
            plans.size()));
  }

  private AiMemoryCandidatesResult extractMemoryCandidates(
      ConversationMemoryGenerationRequest request, List<AiFreeTalkMemoryContext> existingMemories) {
    return aiClient.extractMemoryCandidates(
        new AiMemoryCandidatesRequest(
            request.learningSessionId(),
            request.characterId(),
            request.targetLocale(),
            request.baseLocale(),
            request.timezone(),
            request.history(),
            existingMemories,
            request.followUpContext().askedMemoryIds(),
            request.followUpContext().sessionEndedBy()));
  }
}
