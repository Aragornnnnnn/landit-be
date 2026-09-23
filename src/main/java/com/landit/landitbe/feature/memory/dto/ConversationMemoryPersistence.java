// 장기기억 저장 결과와 새로 저장한 기억의 ID를 함께 전달한다.

package com.landit.landitbe.feature.memory.dto;

import com.landit.landitbe.feature.memory.service.ConversationMemoryWriteService.PersistenceResult;
import java.util.Map;

/**
 * 장기기억 저장 결과다.
 *
 * @param result snapshot 검증 결과
 * @param savedMemoryIdsByPlanIndex 저장 계획 순번(0부터)별 새 기억 ID. IGNORE라 저장하지 않은 계획과 STALE일 때는 들어 있지 않다
 */
public record ConversationMemoryPersistence(
    PersistenceResult result, Map<Integer, Long> savedMemoryIdsByPlanIndex) {

  /** 새 기억 ID 목록을 불변으로 보관한다. */
  public ConversationMemoryPersistence {
    savedMemoryIdsByPlanIndex = Map.copyOf(savedMemoryIdsByPlanIndex);
  }
}
