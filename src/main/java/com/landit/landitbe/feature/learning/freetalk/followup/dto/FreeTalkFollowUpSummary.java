// 스몰톡 요약 마지막에 보여 줄 후속 질문과 그 준비 상태를 전달한다.

package com.landit.landitbe.feature.learning.freetalk.followup.dto;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;
import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType;

/**
 * 요약 화면에 내려 줄 후속 질문이다. 저장된 문구를 그대로 담고 조회할 때 다시 만들지 않는다.
 *
 * @param pending 세션 종료 후 작업이 아직 끝나지 않아 질문이 생길 수 있으면 true. 예: true
 * @param triggerType 질문의 계기. 질문이 없으면 null. 예: CONCERN
 * @param question 굵게 보여 줄 질문. 질문이 없으면 null. 예: "저번에 말한 면접 준비, 어떻게 됐어?"
 * @param invite 다음 스몰톡으로 부르는 한 줄. 질문이 없으면 null. 예: "다음엔 그 얘기 하자. 궁금해."
 */
public record FreeTalkFollowUpSummary(
    boolean pending, FreeTalkFollowUpTriggerType triggerType, String question, String invite) {

  /** 작업이 아직 끝나지 않아 질문을 기다리는 상태다. */
  public static FreeTalkFollowUpSummary waiting() {
    return new FreeTalkFollowUpSummary(true, null, null, null);
  }

  /** 작업은 끝났지만 보여 줄 질문이 없는 상태다. */
  public static FreeTalkFollowUpSummary none() {
    return new FreeTalkFollowUpSummary(false, null, null, null);
  }

  /** 저장된 질문을 그대로 옮긴다. */
  public static FreeTalkFollowUpSummary of(FreeTalkFollowUp followUp) {
    return new FreeTalkFollowUpSummary(
        false, followUp.getTriggerType(), followUp.getQuestion(), followUp.getInvite());
  }
}
