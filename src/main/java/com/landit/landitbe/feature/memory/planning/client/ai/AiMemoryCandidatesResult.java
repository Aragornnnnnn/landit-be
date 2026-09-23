// 장기기억 후보 추출 AI 응답을 담는다.

package com.landit.landitbe.feature.memory.planning.client.ai;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 장기기억 후보 추출 AI 응답을 담는다.
 *
 * @param extractorVersion 후보 추출기 버전
 * @param candidates 추출된 장기기억 후보
 * @param followUpQuestion 같은 응답에 실려 온 다음 스몰톡 후속 질문. 구버전 AI 서버 응답에는 없다
 */
public record AiMemoryCandidatesResult(
    String extractorVersion, List<Candidate> candidates, FollowUpQuestion followUpQuestion) {

  /** 후속 질문이 없는 응답을 만든다. */
  public AiMemoryCandidatesResult(String extractorVersion, List<Candidate> candidates) {
    this(extractorVersion, candidates, null);
  }

  /**
   * 다음 스몰톡에서 이어 물을 질문이다. 근거는 기존 기억이나 이번 후보 중 하나이고, 기본 문구(NONE)면 둘 다 없다.
   *
   * <p>계기는 문자열로 받아 모르는 값이 기억 후보 응답 전체의 역직렬화를 실패시키지 않게 한다.
   *
   * @param memoryId 근거가 된 기존 장기기억 ID
   * @param candidateIndex 근거가 된 이번 후보의 순번
   * @param triggerType 계기(CUT_OFF, PAST_EVENT, CONCERN, GOAL, MOOD, HOBBY, NONE)
   * @param question 질문 문구
   * @param invite 초대 문구
   */
  public record FollowUpQuestion(
      Long memoryId, Integer candidateIndex, String triggerType, String question, String invite) {}

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
