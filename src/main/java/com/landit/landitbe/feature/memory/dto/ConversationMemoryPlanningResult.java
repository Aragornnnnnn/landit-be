// 장기기억 저장 계획과 같은 AI 응답에 실려 온 후속 질문을 함께 전달한다.

package com.landit.landitbe.feature.memory.dto;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import java.util.List;

/**
 * 장기기억 후보 판정 결과다.
 *
 * @param plans 검증된 기억 저장 계획
 * @param followUp 구조 검증을 통과한 후속 질문. AI가 주지 않았거나 계약과 달라 버렸으면 null
 */
public record ConversationMemoryPlanningResult(
    List<ConversationMemoryResolutionPlan> plans, ConversationMemoryFollowUpDraft followUp) {

  /** 계획 목록을 불변으로 보관한다. */
  public ConversationMemoryPlanningResult {
    plans = List.copyOf(plans);
  }
}
