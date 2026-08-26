// 장기기억의 snapshot 재검증과 상태 변경을 하나의 트랜잭션으로 수행한다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryMatch;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryRepository;
import com.landit.landitbe.feature.memory.repository.ConversationMemorySearchRepository;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.feature.session.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.session.service.FreeTalkMemoryGenerationContextService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장기기억의 snapshot 재검증과 상태 변경을 하나의 트랜잭션으로 수행한다. */
@RequiredArgsConstructor
@Service
public class ConversationMemoryWriteService {

  private static final int MAX_COMPARABLE_MEMORIES = 3;

  private final UserProfileRepository userProfileRepository;
  private final ConversationMemoryRepository memoryRepository;
  private final ConversationMemorySearchRepository searchRepository;
  private final FreeTalkMemoryGenerationContextService contextService;

  /** 저장 결과가 snapshot 검증을 통과했는지 나타낸다. */
  public enum PersistenceResult {
    /** 기억과 READY 상태를 저장했다. */
    STORED,

    /** 비교 검색 결과가 변경되어 아무것도 저장하지 않았다. */
    STALE
  }

  /**
   * 사용자 잠금 후 비교 검색 snapshot을 재검증하고 기억 상태를 원자적으로 저장한다.
   *
   * @param learningSessionId READY로 전환할 프리톡 학습 세션 ID
   * @param userProfileId 잠글 사용자 프로필 ID
   * @param plans 후보별 상태 판정과 비교 검색 snapshot
   * @return snapshot이 같아 저장을 완료했으면 STORED, 달라졌으면 STALE
   * @throws ApiException 활성 사용자 프로필이 없을 때
   * @throws IllegalArgumentException 후보 계획이 사용자 범위 또는 상태 계약에 맞지 않을 때
   * @throws IllegalStateException 대체 대상이 활성 상태가 아닐 때
   */
  @Transactional
  public PersistenceResult persistIfSnapshotCurrent(
      long learningSessionId, long userProfileId, List<ConversationMemoryResolutionPlan> plans) {
    requirePositive(learningSessionId, "학습 세션 ID");
    requirePositive(userProfileId, "사용자 프로필 ID");
    if (plans == null) {
      throw new IllegalArgumentException("장기기억 판정 계획이 필요합니다.");
    }

    userProfileRepository
        .findActiveByIdForUpdate(userProfileId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    validatePlans(userProfileId, plans);
    if (snapshotChanged(plans)) {
      return PersistenceResult.STALE;
    }

    for (ConversationMemoryResolutionPlan plan : plans) {
      persistPlan(plan);
    }
    contextService.complete(learningSessionId);
    return PersistenceResult.STORED;
  }

  private boolean snapshotChanged(List<ConversationMemoryResolutionPlan> plans) {
    for (ConversationMemoryResolutionPlan plan : plans) {
      NewConversationMemory memory = plan.memory();
      List<Long> currentIds =
          searchRepository
              .searchActiveComparable(
                  memory.embedding(),
                  memory.userProfileId(),
                  memory.characterId(),
                  memory.memoryType(),
                  MAX_COMPARABLE_MEMORIES)
              .stream()
              .map(ConversationMemoryMatch::memoryId)
              .toList();
      if (!currentIds.equals(plan.snapshotMemoryIds())) {
        return true;
      }
    }
    return false;
  }

  private void persistPlan(ConversationMemoryResolutionPlan plan) {
    if (plan.operation() == AiMemoryOperation.IGNORE) {
      return;
    }

    long newMemoryId = memoryRepository.save(plan.memory(), plan.sourceMessageIds());
    if (plan.operation() == AiMemoryOperation.ADD) {
      return;
    }

    LocalDateTime supersededAt = LocalDateTime.now();
    for (Long oldMemoryId : plan.supersededMemoryIds()) {
      if (!memoryRepository.supersedeActive(
          oldMemoryId, newMemoryId, plan.memory().validFrom(), supersededAt)) {
        throw new IllegalStateException("대체할 활성 장기기억이 없습니다.");
      }
    }
  }

  private static void validatePlans(
      long userProfileId, List<ConversationMemoryResolutionPlan> plans) {
    for (ConversationMemoryResolutionPlan plan : plans) {
      if (plan == null) {
        throw new IllegalArgumentException("장기기억 판정 계획이 유효하지 않습니다.");
      }
      NewConversationMemory memory = plan.memory();
      if (memory.userProfileId() != userProfileId) {
        throw new IllegalArgumentException("장기기억 사용자 범위가 일치하지 않습니다.");
      }
      if (plan.operation() == AiMemoryOperation.SUPERSEDE && plan.supersededMemoryIds().isEmpty()) {
        throw new IllegalArgumentException("대체할 장기기억이 필요합니다.");
      }
      if (plan.operation() == AiMemoryOperation.SUPERSEDE
          && !plan.snapshotMemoryIds().containsAll(plan.supersededMemoryIds())) {
        throw new IllegalArgumentException("대체할 장기기억이 비교 검색 snapshot에 없습니다.");
      }
      if (plan.operation() != AiMemoryOperation.SUPERSEDE
          && !plan.supersededMemoryIds().isEmpty()) {
        throw new IllegalArgumentException("ADD·IGNORE에는 대체할 장기기억이 없어야 합니다.");
      }
    }
  }

  private static void requirePositive(long value, String fieldName) {
    if (value <= 0) {
      throw new IllegalArgumentException(fieldName + "이(가) 유효하지 않습니다.");
    }
  }
}
