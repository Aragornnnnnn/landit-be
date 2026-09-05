// 프리톡 장기기억 후보와 비교 대상의 resolution 입력을 함께 보관한다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import com.landit.landitbe.feature.memory.repository.ConversationMemoryMatch;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionRequest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

record FreeTalkMemoryCandidate(
    int candidateIndex,
    NewConversationMemory memory,
    AiMemoryResolutionRequest.Candidate resolutionCandidate,
    List<ConversationMemoryMatch> comparableMemories) {

  FreeTalkMemoryCandidate {
    comparableMemories = List.copyOf(comparableMemories);
  }

  /**
   * 검색된 비교 기억을 상태 판정 입력에 동일한 시간대 규칙으로 반영한 새 후보를 만든다.
   *
   * @param comparableMemories 후보와 비교할 활성 장기기억 목록
   * @param zoneId 비교 기억의 로컬 시각을 변환할 시간대
   * @return 비교 기억이 반영된 새 후보
   * @throws IllegalArgumentException 비교 기억이 ID, 내용 또는 유효 기간 계약에 맞지 않을 때
   */
  FreeTalkMemoryCandidate withComparable(
      List<ConversationMemoryMatch> comparableMemories, ZoneId zoneId) {
    validateComparableMemories(comparableMemories);
    AiMemoryResolutionRequest.Candidate resolutionCandidate =
        new AiMemoryResolutionRequest.Candidate(
            resolutionCandidate().candidateIndex(),
            resolutionCandidate().content(),
            resolutionCandidate().memoryType(),
            resolutionCandidate().sourceMessageIds(),
            resolutionCandidate().sourceMessages(),
            resolutionCandidate().observedAt(),
            comparableMemories.stream().map(memory -> toComparable(memory, zoneId)).toList());
    return new FreeTalkMemoryCandidate(
        candidateIndex, memory, resolutionCandidate, comparableMemories);
  }

  /** 비교 대상도 대체 계획의 근거이므로 ID·본문·시각 불변식을 다시 확인한다. */
  private static void validateComparableMemories(List<ConversationMemoryMatch> memories) {
    Set<Long> ids = new HashSet<>();
    for (ConversationMemoryMatch memory : memories) {
      if (memory == null
          || memory.memoryId() <= 0
          || !ids.add(memory.memoryId())
          || memory.content() == null
          || memory.content().isBlank()
          || memory.content().length() > 500
          || memory.observedAt() == null
          || invalidValidityRange(memory.validFrom(), memory.validTo())) {
        throw new IllegalArgumentException("비교 장기기억 계약이 유효하지 않습니다.");
      }
    }
  }

  private static AiMemoryResolutionRequest.ComparableMemory toComparable(
      ConversationMemoryMatch memory, ZoneId zoneId) {
    return new AiMemoryResolutionRequest.ComparableMemory(
        memory.memoryId(),
        memory.content(),
        toOffset(memory.validFrom(), zoneId),
        toOffset(memory.validTo(), zoneId),
        toOffset(memory.observedAt(), zoneId));
  }

  private static OffsetDateTime toOffset(LocalDateTime value, ZoneId zoneId) {
    return value == null ? null : value.atZone(zoneId).toOffsetDateTime();
  }

  private static boolean invalidValidityRange(LocalDateTime validFrom, LocalDateTime validTo) {
    return validFrom != null && validTo != null && validTo.isBefore(validFrom);
  }
}
