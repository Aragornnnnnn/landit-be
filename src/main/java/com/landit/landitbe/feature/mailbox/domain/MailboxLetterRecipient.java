// 답장 편지를 사용자별로 전달하고 읽음 상태를 저장하는 Entity다.

package com.landit.landitbe.feature.mailbox.domain;

import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

/** 답장 편지를 사용자별로 전달하고 읽음 상태를 저장하는 Entity다. */
@Getter
@Entity
@Table(name = "mailbox_letter_recipient")
public class MailboxLetterRecipient extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "letter_id", nullable = false)
  private Long letterId;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "representative_feedback_id", nullable = false)
  private Long representativeFeedbackId;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected MailboxLetterRecipient() {}

  /**
   * 답장 수신 정보를 생성한다.
   *
   * @param letterId 답장 편지 ID
   * @param userProfileId 수신 사용자 ID
   * @param representativeFeedbackId 답장과 연결할 대표 피드백 ID
   */
  public MailboxLetterRecipient(Long letterId, Long userProfileId, Long representativeFeedbackId) {
    this.letterId = letterId;
    this.userProfileId = userProfileId;
    this.representativeFeedbackId = representativeFeedbackId;
  }
}
