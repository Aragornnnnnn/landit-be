// 프리톡 장기기억 후보의 비교 검색과 AI resolution 결과를 저장 계획으로 바꾼다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.session.service.FreeTalkMemoryGenerationContextService;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class FreeTalkMemoryResolutionPlanner {

  private static final int MAX_COMPARABLE_MEMORIES = 3;

  private final AiFreeTalkClient aiClient;
  private final ConversationMemorySearchRepository searchRepository;
  private final Clock clock;

  List<ConversationMemoryResolutionPlan> plan(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      List<FreeTalkMemoryCandidate> candidates) {
    if (candidates.isEmpty()) {
      return List.of();
    }
    List<FreeTalkMemoryCandidate> candidatesWithComparables =
        candidates.stream().map(candidate -> addComparables(context, candidate)).toList();
    if (isSingleNewCandidate(candidatesWithComparables)) {
      return List.of(addPlan(candidatesWithComparables.getFirst()));
    }
    AiMemoryResolutionResult resolution =
        aiClient.resolveMemory(
            new AiMemoryResolutionRequest(
                candidatesWithComparables.stream()
                    .map(FreeTalkMemoryCandidate::resolutionCandidate)
                    .toList()));
    return plansFromResolutions(candidatesWithComparables, resolution);
  }

  private FreeTalkMemoryCandidate addComparables(
      FreeTalkMemoryGenerationContextService.GenerationContext context,
      FreeTalkMemoryCandidate candidate) {
    List<ConversationMemoryMatch> comparable =
        searchRepository.searchActiveComparable(
            candidate.memory().embedding(),
            context.userProfileId(),
            candidate.memory().characterId(),
            candidate.memory().memoryType(),
            MAX_COMPARABLE_MEMORIES);
    if (comparable == null || comparable.size() > MAX_COMPARABLE_MEMORIES) {
      throw new IllegalArgumentException("비교 장기기억 목록이 유효하지 않습니다.");
    }
    return candidate.withComparable(comparable, clock.getZone());
  }

  private static boolean isSingleNewCandidate(List<FreeTalkMemoryCandidate> candidates) {
    return candidates.size() == 1 && candidates.getFirst().comparableMemories().isEmpty();
  }

  private static ConversationMemoryResolutionPlan addPlan(FreeTalkMemoryCandidate candidate) {
    return new ConversationMemoryResolutionPlan(
        candidate.memory(),
        candidate.resolutionCandidate().sourceMessageIds(),
        List.of(),
        AiMemoryOperation.ADD,
        List.of());
  }

  private static List<ConversationMemoryResolutionPlan> plansFromResolutions(
      List<FreeTalkMemoryCandidate> candidates, AiMemoryResolutionResult result) {
    validateResolutionCount(candidates, result);
    Map<Integer, FreeTalkMemoryCandidate> candidatesByIndex = candidatesByIndex(candidates);
    Map<Integer, AiMemoryResolutionResult.Resolution> resolutions = new HashMap<>();
    Set<Long> supersededAcrossCandidates = new HashSet<>();
    for (AiMemoryResolutionResult.Resolution resolution : result.resolutions()) {
      validateResolution(resolution, candidatesByIndex, resolutions);
      FreeTalkMemoryCandidate candidate = candidatesByIndex.get(resolution.candidateIndex());
      Set<Long> comparableIds =
          candidate.comparableMemories().stream()
              .map(ConversationMemoryMatch::memoryId)
              .collect(Collectors.toSet());
      if (invalidSupersededIds(
          resolution.operation(),
          resolution.supersededMemoryIds(),
          comparableIds,
          supersededAcrossCandidates)) {
        throw new IllegalArgumentException("장기기억 대체 대상이 유효하지 않습니다.");
      }
    }
    if (resolutions.size() != candidates.size()) {
      throw new IllegalArgumentException("모든 장기기억 후보의 상태 판정이 필요합니다.");
    }
    return candidates.stream()
        .map(candidate -> toPlan(candidate, resolutions.get(candidate.candidateIndex())))
        .toList();
  }

  private static void validateResolutionCount(
      List<FreeTalkMemoryCandidate> candidates, AiMemoryResolutionResult result) {
    if (result == null
        || result.resolutions() == null
        || result.resolutions().size() != candidates.size()) {
      throw new IllegalArgumentException("장기기억 상태 판정 응답 개수가 유효하지 않습니다.");
    }
  }

  private static Map<Integer, FreeTalkMemoryCandidate> candidatesByIndex(
      List<FreeTalkMemoryCandidate> candidates) {
    Map<Integer, FreeTalkMemoryCandidate> candidatesByIndex = new HashMap<>();
    for (FreeTalkMemoryCandidate candidate : candidates) {
      if (candidatesByIndex.put(candidate.candidateIndex(), candidate) != null) {
        throw new IllegalArgumentException("장기기억 후보 인덱스가 중복됐습니다.");
      }
    }
    return candidatesByIndex;
  }

  private static void validateResolution(
      AiMemoryResolutionResult.Resolution resolution,
      Map<Integer, FreeTalkMemoryCandidate> candidatesByIndex,
      Map<Integer, AiMemoryResolutionResult.Resolution> resolutions) {
    if (resolution == null
        || resolution.candidateIndex() == null
        || resolution.operation() == null
        || resolution.supersededMemoryIds() == null
        || candidatesByIndex.get(resolution.candidateIndex()) == null
        || resolutions.put(resolution.candidateIndex(), resolution) != null) {
      throw new IllegalArgumentException("장기기억 상태 판정 응답이 유효하지 않습니다.");
    }
  }

  private static ConversationMemoryResolutionPlan toPlan(
      FreeTalkMemoryCandidate candidate, AiMemoryResolutionResult.Resolution resolution) {
    return new ConversationMemoryResolutionPlan(
        candidate.memory(),
        candidate.resolutionCandidate().sourceMessageIds(),
        candidate.comparableMemories().stream().map(ConversationMemoryMatch::memoryId).toList(),
        resolution.operation(),
        resolution.supersededMemoryIds());
  }

  private static boolean invalidSupersededIds(
      AiMemoryOperation operation, List<Long> ids, Set<Long> comparableIds, Set<Long> globalIds) {
    if (ids.stream().anyMatch(id -> id == null || id <= 0)
        || ids.size() != new HashSet<>(ids).size()
        || !comparableIds.containsAll(ids)
        || ids.stream().anyMatch(id -> !globalIds.add(id))) {
      return true;
    }
    return operation == AiMemoryOperation.SUPERSEDE ? ids.isEmpty() : !ids.isEmpty();
  }
}
