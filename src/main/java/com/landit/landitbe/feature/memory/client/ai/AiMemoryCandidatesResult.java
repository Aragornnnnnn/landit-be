// 장기기억 후보 추출 AI 응답을 담는다.

package com.landit.landitbe.feature.memory.client.ai;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.time.OffsetDateTime;
import java.util.List;

/** 장기기억 후보 추출 AI 응답을 담는다. */
public record AiMemoryCandidatesResult(String extractorVersion, List<Candidate> candidates) {

  /** 추출된 장기기억 후보와 원본 메시지 정보를 담는다. */
  public record Candidate(
      Integer candidateIndex,
      ConversationMemoryType memoryType,
      String content,
      String contentLocale,
      List<Long> sourceMessageIds,
      Double confidence,
      OffsetDateTime validFrom,
      OffsetDateTime validTo,
      String embeddingModel,
      List<Float> embedding) {}
}
