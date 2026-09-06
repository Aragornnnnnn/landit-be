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

  /**
   * 장기기억으로 검토할 후보 한 건을 담는다.
   *
   * @param candidateIndex 후보 목록에서의 0부터 시작하는 순번
   * @param memoryType 장기기억 의미 유형
   * @param content 저장할 기억 본문
   * @param contentLocale 기억 본문의 언어 지역 태그
   * @param sourceMessageIds 기억의 근거가 되는 원본 메시지 ID 목록
   * @param confidence 후보 추출 신뢰도
   * @param validFrom 기억이 유효해진 시각
   * @param validTo 기억이 유효한 마지막 시각. 현재 유효하면 null이다.
   * @param embeddingModel 임베딩 모델 식별자
   * @param embedding 기억 본문의 임베딩
   */
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
