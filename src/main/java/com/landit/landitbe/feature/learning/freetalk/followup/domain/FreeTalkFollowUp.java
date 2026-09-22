// 스몰톡 요약 마지막에 보여 줄 "다음 스몰톡에서" 후속 질문을 저장한다.

package com.landit.landitbe.feature.learning.freetalk.followup.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 한 스몰톡 세션이 끝난 뒤 만들어진 후속 질문이다.
 *
 * <p>다음에도 스몰톡을 열고 싶게 만드는 한 줄이다. 세션마다 하나만 두고, AI가 만든 문구를 저장 시점 그대로 남긴다.
 */
@Getter
@Entity
@Table(name = "free_talk_follow_up")
public class FreeTalkFollowUp extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 예: 812

  @Column(name = "user_profile_id", nullable = false, updatable = false)
  private Long userProfileId; // 예: 1207

  @Column(name = "free_talk_session_id", nullable = false, updatable = false)
  private Long freeTalkSessionId; // 예: 30 (질문을 만든 세션)

  @Column(name = "memory_id", updatable = false)
  private Long memoryId; // 예: 5504. 기본 문구(NONE)면 null

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
  private FreeTalkFollowUpTriggerType triggerType; // 예: CONCERN

  // 예: "저번에 말한 면접 준비, 어떻게 됐어?"
  @Column(name = "question", nullable = false, columnDefinition = "text", updatable = false)
  private String question;

  // 예: "다음엔 그 얘기 하자. 궁금해."
  @Column(name = "invite", nullable = false, columnDefinition = "text", updatable = false)
  private String invite;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkFollowUp() {}

  private FreeTalkFollowUp(
      Long userProfileId,
      Long freeTalkSessionId,
      Long memoryId,
      FreeTalkFollowUpTriggerType triggerType,
      String question,
      String invite) {
    this.userProfileId = userProfileId;
    this.freeTalkSessionId = freeTalkSessionId;
    this.memoryId = memoryId;
    this.triggerType = triggerType;
    this.question = question;
    this.invite = invite;
  }

  /**
   * 세션이 끝난 뒤 만들어진 후속 질문을 만든다.
   *
   * @param userProfileId 질문을 받을 사용자 프로필 ID
   * @param freeTalkSessionId 질문을 만든 프리톡 세션 ID
   * @param memoryId 질문의 근거가 된 장기기억 ID. 근거가 없거나 기억으로 저장되지 않았으면 null
   * @param triggerType 질문의 계기
   * @param question 굵게 보여 줄 질문
   * @param invite 다음 스몰톡으로 부르는 한 줄
   * @return 저장할 후속 질문
   * @throws IllegalArgumentException 문구가 비었거나 기본 문구(NONE)에 근거 기억이 있을 때
   */
  public static FreeTalkFollowUp of(
      long userProfileId,
      long freeTalkSessionId,
      Long memoryId,
      FreeTalkFollowUpTriggerType triggerType,
      String question,
      String invite) {
    if (triggerType == null
        || question == null
        || question.isBlank()
        || invite == null
        || invite.isBlank()) {
      throw new IllegalArgumentException("후속 질문의 계기와 문구는 필수입니다.");
    }
    if (triggerType == FreeTalkFollowUpTriggerType.NONE && memoryId != null) {
      throw new IllegalArgumentException("기본 문구는 근거 기억을 가질 수 없습니다.");
    }
    return new FreeTalkFollowUp(
        userProfileId, freeTalkSessionId, memoryId, triggerType, question, invite);
  }
}
