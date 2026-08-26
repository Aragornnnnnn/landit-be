// 장기기억 후보 추출 AI 응답을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 장기기억 후보 추출 결과를 담는다.
 *
 * @param extractorVersion 후보 추출기 버전
 * @param candidates 후보 목록
 */
public record AiMemoryCandidatesResult(String extractorVersion, List<Candidate> candidates) {

  /** 장기기억으로 검토할 후보 한 건을 담는다. */
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
