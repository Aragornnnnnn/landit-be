// 사용자가 편지함에 제출한 피드백을 저장하는 Entity다.

package com.landit.landitbe.feature.mailbox.domain;

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

/** 사용자가 편지함에 제출한 피드백을 저장하는 Entity다. */
@Getter
@Entity
@Table(name = "mailbox_feedback")
public class MailboxFeedback extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Enumerated(EnumType.STRING)
  @Column(name = "feedback_type", nullable = false, length = 30)
  private UserFeedbackType feedbackType;

  @Column(name = "content_text", nullable = false, columnDefinition = "text")
  private String contentText;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 20)
  private UserFeedbackStatus processingStatus;

  @Column(name = "resolved_by_feedback_id")
  private Long resolvedByFeedbackId;

  /** JPA에서 사용하는 기본 생성자다. */
  protected MailboxFeedback() {}

  /**
   * 사용자가 제출한 새 피드백을 생성한다.
   *
   * @param userProfileId 피드백을 제출한 사용자 ID
   * @param feedbackType 피드백 유형
   * @param contentText 피드백 본문
   */
  public MailboxFeedback(Long userProfileId, UserFeedbackType feedbackType, String contentText) {
    this.userProfileId = userProfileId;
    this.feedbackType = feedbackType;
    this.contentText = contentText;
    this.processingStatus = UserFeedbackStatus.PENDING;
  }
}
