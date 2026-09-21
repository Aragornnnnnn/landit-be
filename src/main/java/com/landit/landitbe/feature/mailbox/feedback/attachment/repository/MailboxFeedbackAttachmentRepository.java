// 문의별 첨부 메타데이터를 순서대로 조회하고 저장한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.repository;

import com.landit.landitbe.feature.mailbox.feedback.attachment.domain.MailboxFeedbackAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 문의별 첨부 메타데이터를 조회하고 저장한다. */
public interface MailboxFeedbackAttachmentRepository
    extends JpaRepository<MailboxFeedbackAttachment, Long> {

  /**
   * 문의의 첨부를 업로드 순서대로 조회한다.
   *
   * @param feedbackId 문의 ID
   * @return 첨부 목록
   */
  List<MailboxFeedbackAttachment> findByFeedbackIdOrderByDisplayOrder(Long feedbackId);

  /**
   * 해당 문의에 연결된 첨부만 조회한다.
   *
   * @param id 첨부 ID
   * @param feedbackId 문의 ID
   * @return 연결된 첨부
   */
  Optional<MailboxFeedbackAttachment> findByIdAndFeedbackId(Long id, Long feedbackId);
}
