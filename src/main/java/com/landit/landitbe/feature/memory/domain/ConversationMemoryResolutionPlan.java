// 장기기억 후보에 적용할 상태 판정과 검색 snapshot을 함께 표현한다.

package com.landit.landitbe.feature.memory.domain;

import com.landit.landitbe.feature.session.client.ai.AiMemoryOperation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 장기기억 후보 저장에 필요한 상태 판정과 비교 검색 snapshot을 표현한다. */
public record ConversationMemoryResolutionPlan(
    NewConversationMemory memory,
    List<Long> sourceMessageIds,
    List<Long> snapshotMemoryIds,
    AiMemoryOperation operation,
    List<Long> supersededMemoryIds) {

  /** 입력 목록을 방어적으로 복사하고 상태 판정에 필요한 기본 불변식을 검증한다. */
  public ConversationMemoryResolutionPlan {
    if (memory == null || operation == null) {
      throw new IllegalArgumentException("장기기억 판정 계획의 기억과 연산은 필수입니다.");
    }
    sourceMessageIds = copyIds(sourceMessageIds, "원본 메시지");
    snapshotMemoryIds = copyIds(snapshotMemoryIds, "비교 검색 snapshot");
    supersededMemoryIds = copyIds(supersededMemoryIds, "대체할 장기기억");
  }

  private static List<Long> copyIds(List<Long> ids, String fieldName) {
    if (ids == null) {
      throw new IllegalArgumentException(fieldName + " 목록이 필요합니다.");
    }
    Set<Long> uniqueIds = new HashSet<>();
    for (Long id : ids) {
      if (id == null || id <= 0 || !uniqueIds.add(id)) {
        throw new IllegalArgumentException(fieldName + " ID가 유효하지 않습니다.");
      }
    }
    return List.copyOf(ids);
  }
}
