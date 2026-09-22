// 문의 첨부 메타데이터를 조회하고 작성자·관리자에게만 이미지 바이트를 제공한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.service;

import com.landit.landitbe.feature.mailbox.feedback.attachment.client.MailboxAttachmentClient;
import com.landit.landitbe.feature.mailbox.feedback.attachment.domain.MailboxFeedbackAttachment;
import com.landit.landitbe.feature.mailbox.feedback.attachment.dto.MailboxAttachmentImage;
import com.landit.landitbe.feature.mailbox.feedback.attachment.dto.MailboxFeedbackAttachmentResponse;
import com.landit.landitbe.feature.mailbox.feedback.attachment.repository.MailboxFeedbackAttachmentRepository;
import com.landit.landitbe.feature.mailbox.feedback.repository.MailboxFeedbackRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 문의 첨부 메타데이터와 권한을 확인한 이미지 조회를 담당한다. */
@Service
@RequiredArgsConstructor
public class MailboxFeedbackAttachmentService {

  private final MailboxFeedbackAttachmentRepository attachmentRepository;
  private final MailboxFeedbackRepository feedbackRepository;
  private final UserProfileService userProfileService;
  private final MailboxAttachmentClient attachmentClient;

  /**
   * 접근 권한이 확인된 문의의 첨부 메타데이터를 조회한다.
   *
   * @param feedbackId 호출자가 조회 권한을 확인한 문의 ID
   * @return 첨부 순서대로 정렬된 목록. 첨부가 없으면 빈 배열
   */
  @Transactional(readOnly = true)
  public List<MailboxFeedbackAttachmentResponse> getAttachments(Long feedbackId) {
    return attachmentRepository.findByFeedbackIdOrderByDisplayOrder(feedbackId).stream()
        .map(
            attachment ->
                new MailboxFeedbackAttachmentResponse(
                    attachment.getId(),
                    attachment.getContentType(),
                    attachment.getFileSize(),
                    "/api/v1/mailbox/feedbacks/%d/attachments/%d"
                        .formatted(feedbackId, attachment.getId())))
        .toList();
  }

  /**
   * 문의 작성자 또는 관리자에게 해당 문의에 연결된 이미지를 제공한다.
   *
   * @param userId 인증된 사용자 ID
   * @param feedbackId 문의 ID
   * @param attachmentId 첨부 ID
   * @return 이미지 바이트와 MIME 유형
   * @throws ApiException 첨부가 없거나 권한이 없는 경우, 저장소 조회가 실패한 경우
   */
  public MailboxAttachmentImage download(Long userId, Long feedbackId, Long attachmentId) {
    if (feedbackRepository.findByIdAndUserProfileId(feedbackId, userId).isEmpty()
        && !userProfileService.isAdmin(userId)) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    MailboxFeedbackAttachment attachment =
        attachmentRepository
            .findByIdAndFeedbackId(attachmentId, feedbackId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    return new MailboxAttachmentImage(
        attachmentClient.download(attachment.getObjectKey(), attachment.getFileSize()),
        attachment.getContentType());
  }
}
