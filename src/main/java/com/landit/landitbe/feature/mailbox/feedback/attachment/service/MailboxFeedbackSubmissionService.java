// 이미지를 먼저 업로드한 뒤 문의와 첨부를 함께 확정하고 실패하면 업로드를 정리한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.service;

import com.landit.landitbe.feature.mailbox.feedback.attachment.client.MailboxAttachmentClient;
import com.landit.landitbe.feature.mailbox.feedback.attachment.domain.MailboxFeedbackAttachment;
import com.landit.landitbe.feature.mailbox.feedback.attachment.dto.MailboxAttachmentImage;
import com.landit.landitbe.feature.mailbox.feedback.attachment.repository.MailboxFeedbackAttachmentRepository;
import com.landit.landitbe.feature.mailbox.feedback.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.feedback.dto.MailboxFeedbackSubmitRequest;
import com.landit.landitbe.feature.mailbox.feedback.repository.MailboxFeedbackRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 이미지 업로드와 문의 DB 확정을 조율하고 실패 시 해당 요청의 객체만 정리한다. */
@Slf4j
@Service
public class MailboxFeedbackSubmissionService {

  private final MailboxAttachmentImageService imageService;
  private final MailboxAttachmentClient attachmentClient;
  private final MailboxFeedbackRepository feedbackRepository;
  private final MailboxFeedbackAttachmentRepository attachmentRepository;
  private final UserProfileService userProfileService;
  private final TransactionTemplate transaction;

  /**
   * 검증·저장 의존성과 DB 확정 트랜잭션을 구성한다.
   *
   * @param imageService 이미지 검증 서비스
   * @param attachmentClient 비공개 이미지 저장소
   * @param feedbackRepository 문의 저장소
   * @param attachmentRepository 첨부 저장소
   * @param userProfileService 활성 사용자 확인 서비스
   * @param transactionManager DB 트랜잭션 관리자
   */
  public MailboxFeedbackSubmissionService(
      MailboxAttachmentImageService imageService,
      MailboxAttachmentClient attachmentClient,
      MailboxFeedbackRepository feedbackRepository,
      MailboxFeedbackAttachmentRepository attachmentRepository,
      UserProfileService userProfileService,
      PlatformTransactionManager transactionManager) {
    this.imageService = imageService;
    this.attachmentClient = attachmentClient;
    this.feedbackRepository = feedbackRepository;
    this.attachmentRepository = attachmentRepository;
    this.userProfileService = userProfileService;
    this.transaction = new TransactionTemplate(transactionManager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * 파일을 검증·업로드하고 문의 및 첨부를 하나의 DB 트랜잭션으로 확정한다.
   *
   * @param userId 인증된 사용자 ID
   * @param request 검증된 문의 요청
   * @param files 첨부 이미지 목록. 생략 가능
   * @throws RuntimeException 업로드 또는 DB 저장 실패 시. 이미 업로드한 파일은 삭제를 시도한다
   */
  public void submit(Long userId, MailboxFeedbackSubmitRequest request, List<MultipartFile> files) {
    List<MailboxAttachmentImage> images = imageService.validate(files);
    List<String> attemptedKeys = new ArrayList<>();
    try {
      for (MailboxAttachmentImage image : images) {
        String key = "mailbox/feedback/" + UUID.randomUUID();
        // 응답이 유실된 PUT도 객체가 생성됐을 수 있으므로 업로드 시도 전에 기록한다.
        attemptedKeys.add(key);
        attachmentClient.upload(key, image.content(), image.contentType());
      }
      transaction.executeWithoutResult(status -> save(userId, request, images, attemptedKeys));
    } catch (RuntimeException exception) {
      attemptedKeys.forEach(this::deleteAfterFailure);
      throw exception;
    }
  }

  private void save(
      Long userId,
      MailboxFeedbackSubmitRequest request,
      List<MailboxAttachmentImage> images,
      List<String> keys) {
    userProfileService.requireActiveForUpdate(userId);
    MailboxFeedback feedback = feedbackRepository.save(request.toEntity(userId));
    for (int index = 0; index < images.size(); index++) {
      MailboxAttachmentImage image = images.get(index);
      attachmentRepository.save(
          new MailboxFeedbackAttachment(
              feedback.getId(),
              keys.get(index),
              image.contentType(),
              image.content().length,
              index));
    }
  }

  private void deleteAfterFailure(String key) {
    try {
      attachmentClient.delete(key);
    } catch (RuntimeException exception) {
      log.error("미완료 문의 첨부 정리에 실패했습니다. objectKey={}", key);
    }
  }
}
