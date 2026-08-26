// 장기기억 후보 상태 판정 AI 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 후보별 비교 대상 장기기억을 포함한 상태 판정 요청을 담는다.
 *
 * @param candidates 상태를 판정할 후보 목록
 */
public record AiMemoryResolutionRequest(List<Candidate> candidates) {

  /** 상태 판정 대상 후보와 비교 대상 기억을 담는다. */
  public record Candidate(
      int candidateIndex,
      String content,
      ConversationMemoryType memoryType,
      List<Long> sourceMessageIds,
      OffsetDateTime observedAt,
      List<ComparableMemory> comparableMemories) {}

  /** 후보와 비교할 기존 활성 장기기억을 담는다. */
  public record ComparableMemory(
      Long memoryId,
      String content,
      OffsetDateTime validFrom,
      OffsetDateTime validTo,
      OffsetDateTime observedAt) {}
}
