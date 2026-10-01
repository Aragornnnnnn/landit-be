// AI가 스몰톡 첫 발화에서 물어볼 저장된 예고 질문을 전달한다.

package com.landit.landitbe.feature.learning.freetalk.followup.dto;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;

/**
 * AI 오프닝 요청에 전달하는 예고 질문이다.
 *
 * @param followUpId 질문 ID
 * @param memoryId 질문의 근거 기억 ID. 없으면 null
 * @param triggerType 질문 계기
 * @param question 저장된 질문 문구
 */
public record AiFreeTalkPendingFollowUp(
    long followUpId, Long memoryId, String triggerType, String question) {

  /**
   * 저장된 질문에서 AI 요청 값을 만든다.
   *
   * @param followUp 저장된 질문
   * @return AI에 전달할 질문
   */
  public static AiFreeTalkPendingFollowUp from(FreeTalkFollowUp followUp) {
    return new AiFreeTalkPendingFollowUp(
        followUp.getId(),
        followUp.getMemoryId(),
        followUp.getTriggerType().name(),
        followUp.getQuestion());
  }
}
