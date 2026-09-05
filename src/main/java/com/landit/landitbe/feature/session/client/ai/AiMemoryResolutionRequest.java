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

  /**
   * 상태 판정 대상 후보와 비교 대상 기억을 담는다.
   *
   * @param candidateIndex 후보 목록에서의 0부터 시작하는 순번
   * @param content 상태를 판정할 기억 본문
   * @param memoryType 장기기억 의미 유형
   * @param sourceMessageIds 기억의 근거가 되는 원본 메시지 ID 목록
   * @param sourceMessages 정정 근거로 사용할 원본 사용자 메시지 목록
   * @param observedAt 기억을 관찰한 시각
   * @param comparableMemories 비교할 기존 활성 장기기억 목록
   */
  public record Candidate(
      int candidateIndex,
      String content,
      ConversationMemoryType memoryType,
      List<Long> sourceMessageIds,
      List<AiConversationHistoryMessage> sourceMessages,
      OffsetDateTime observedAt,
      List<ComparableMemory> comparableMemories) {}

  /**
   * 후보와 비교할 기존 활성 장기기억을 담는다.
   *
   * @param memoryId 비교할 기존 기억 ID
   * @param content 기존 기억 본문
   * @param validFrom 기존 기억이 유효해진 시각
   * @param validTo 기존 기억이 유효한 마지막 시각. 현재 유효하면 null이다.
   * @param observedAt 기존 기억을 관찰한 시각
   */
  public record ComparableMemory(
      Long memoryId,
      String content,
      OffsetDateTime validFrom,
      OffsetDateTime validTo,
      OffsetDateTime observedAt) {}
}
