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
import org.springframework.stereotype.Service;

/** 비교 기억을 조회하고 AI 판정을 완전한 저장 계획으로 검증한다. */
@Service
@RequiredArgsConstructor
final class FreeTalkMemoryResolutionService {

  private static final int MAX_COMPARABLE_MEMORIES = 3;

  private final AiFreeTalkClient aiClient;
  private final ConversationMemorySearchRepository searchRepository;
  private final Clock clock;

  /**
   * 각 후보에 비교 기억을 연결하고, 신규 후보는 추가 계획으로, 기존 기억과 겹치는 후보는 AI 판정 결과에 따른 저장 계획으로 변환한다.
   *
   * @param context 장기기억 생성에 필요한 사용자와 프리톡 세션 정보
   * @param candidates 저장 후보와 원본 메시지 정보
   * @return 후보별 추가, 유지 또는 대체 저장 계획
   * @throws IllegalArgumentException 비교 기억 또는 AI 상태 판정 결과가 계약에 맞지 않을 때
   */
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

  /** 같은 사용자·캐릭터·유형의 활성 기억만 비교 대상으로 제한한다. */
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
    Map<Integer, AiMemoryResolutionResult.Resolution> resolutions =
        validateResolutions(result, candidatesByIndex);
    return candidates.stream()
        .map(candidate -> toPlan(candidate, resolutions.get(candidate.candidateIndex())))
        .toList();
  }

  /** 모든 후보가 정확히 한 번 판정되고 대체 ID가 후보별 비교 범위에 속하는지 확인한다. */
  private static Map<Integer, AiMemoryResolutionResult.Resolution> validateResolutions(
      AiMemoryResolutionResult result, Map<Integer, FreeTalkMemoryCandidate> candidatesByIndex) {
    Map<Integer, AiMemoryResolutionResult.Resolution> resolutions = new HashMap<>();
    Set<Long> supersededAcrossCandidates = new HashSet<>();
    for (AiMemoryResolutionResult.Resolution resolution : result.resolutions()) {
      validateResolution(resolution, candidatesByIndex, resolutions);
      validateSupersededIdsForCandidate(
          resolution,
          candidatesByIndex.get(resolution.candidateIndex()),
          supersededAcrossCandidates);
    }
    if (resolutions.size() != candidatesByIndex.size()) {
      throw new IllegalArgumentException("모든 장기기억 후보의 상태 판정이 필요합니다.");
    }
    return resolutions;
  }

  /** 후보별 비교 범위와 전역 대체 ID 중복을 같은 순서로 검증한다. */
  private static void validateSupersededIdsForCandidate(
      AiMemoryResolutionResult.Resolution resolution,
      FreeTalkMemoryCandidate candidate,
      Set<Long> supersededAcrossCandidates) {
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

  /** 상태 판정 응답은 후보 수와 같아야 누락된 저장 계획을 만들지 않는다. */
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

  /** 대체 ID는 양수·중복 없음·비교 결과의 부분집합이며 후보 간에도 겹치지 않아야 한다. */
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
