// 문의 첨부의 비공개 객체 키와 검증된 이미지 메타데이터를 저장한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.domain;

import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 문의 첨부의 비공개 객체 키와 검증된 이미지 메타데이터다. */
@Entity
@Getter
@Table(name = "mailbox_feedback_attachment")
public class MailboxFeedbackAttachment extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "feedback_id", nullable = false)
  private Long feedbackId;

  @Column(name = "object_key", nullable = false, length = 200)
  private String objectKey;

  @Column(name = "content_type", nullable = false, length = 30)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  /** JPA 전용 기본 생성자다. */
  protected MailboxFeedbackAttachment() {}

  /**
   * 저장된 이미지와 문의의 연결을 생성한다.
   *
   * @param feedbackId 문의 ID
   * @param objectKey 비공개 객체 키
   * @param contentType 검증된 MIME 유형
   * @param fileSize 실제 파일 크기
   * @param displayOrder 첨부 순서
   */
  public MailboxFeedbackAttachment(
      Long feedbackId, String objectKey, String contentType, long fileSize, int displayOrder) {
    this.feedbackId = feedbackId;
    this.objectKey = objectKey;
    this.contentType = contentType;
    this.fileSize = fileSize;
    this.displayOrder = displayOrder;
  }
}
