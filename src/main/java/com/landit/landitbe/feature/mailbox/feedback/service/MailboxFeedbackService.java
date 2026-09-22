// 사용자의 문의 제출과 보낸 문의·답장을 조회한다.

package com.landit.landitbe.feature.mailbox.feedback.service;

import com.landit.landitbe.feature.mailbox.feedback.attachment.service.MailboxFeedbackAttachmentService;
import com.landit.landitbe.feature.mailbox.feedback.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.feedback.dto.MailboxFeedbackSubmitRequest;
import com.landit.landitbe.feature.mailbox.feedback.dto.MailboxSentFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.feedback.dto.MailboxSentFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.feedback.repository.MailboxFeedbackRepository;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterRecipient;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.letter.repository.MailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.MailboxLetterRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자의 문의 제출과 보낸 문의·답장을 조회한다. */
@Service
@RequiredArgsConstructor
public class MailboxFeedbackService {

  private static final int MAX_PAGE_SIZE = 100;

  private final MailboxFeedbackRepository mailboxFeedbackRepository;
  private final MailboxLetterRepository mailboxLetterRepository;
  private final MailboxFeedbackAttachmentService attachmentService;
  private final MailboxLetterRecipientRepository mailboxLetterRecipientRepository;

  /**
   * 인증된 사용자의 새 피드백을 저장한다.
   *
   * @param userProfileId 사용자 ID
   * @param request 피드백 등록 요청
   */
  @Transactional
  public void submitFeedback(Long userProfileId, MailboxFeedbackSubmitRequest request) {
    mailboxFeedbackRepository.save(request.toEntity(userProfileId));
  }

  /**
   * 인증된 사용자의 피드백을 최신순 커서 페이지로 조회한다.
   *
   * @param userProfileId 사용자 ID
   * @param cursor 다음 페이지 커서
   * @param size 페이지 크기
   * @return 보낸 피드백 커서 페이지
   * @throws ApiException 커서나 페이지 크기가 유효하지 않은 경우
   */
  @Transactional(readOnly = true)
  public MailboxSentFeedbackListResponse getSentFeedbacks(
      Long userProfileId, String cursor, int size) {
    validatePageSize(size);
    FeedbackCursor feedbackCursor = decodeFeedbackCursor(cursor);
    List<MailboxFeedback> feedbacks =
        feedbackCursor == null
            ? mailboxFeedbackRepository.findByUserProfileIdOrderByCreatedAtDescIdDesc(
                userProfileId, PageRequest.of(0, size + 1))
            : mailboxFeedbackRepository.findBeforeCursor(
                userProfileId,
                feedbackCursor.createdAt(),
                feedbackCursor.feedbackId(),
                PageRequest.of(0, size + 1));

    boolean hasNext = feedbacks.size() > size;
    if (hasNext) {
      feedbacks = feedbacks.subList(0, size);
    }
    String nextCursor = hasNext ? encodeFeedbackCursor(feedbacks.getLast()) : null;
    return new MailboxSentFeedbackListResponse(
        feedbacks.stream().map(MailboxFeedbackService::toSentFeedbackItem).toList(),
        nextCursor,
        hasNext);
  }

  /**
   * 인증된 사용자의 피드백 상세를 조회한다.
   *
   * @param userProfileId 사용자 ID
   * @param feedbackId 피드백 ID
   * @return 보낸 피드백 상세
   * @throws ApiException 피드백이 없거나 사용자 소유가 아닌 경우
   */
  @Transactional(readOnly = true)
  public MailboxSentFeedbackDetailResponse getSentFeedback(Long userProfileId, Long feedbackId) {
    MailboxFeedback feedback =
        mailboxFeedbackRepository
            .findByIdAndUserProfileId(feedbackId, userProfileId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    return new MailboxSentFeedbackDetailResponse(
        feedback.getId(),
        feedback.getFeedbackType(),
        feedback.getFeedbackType().getDisplayTitle(),
        feedback.getContentText(),
        feedback.getProcessingStatus(),
        feedback.getResolvedByFeedbackId(),
        feedback.getCreatedAt(),
        feedback.getUpdatedAt(),
        getPublishedReplies(feedback.getId(), userProfileId),
        attachmentService.getAttachments(feedback.getId()));
  }

  private static MailboxSentFeedbackListResponse.Item toSentFeedbackItem(MailboxFeedback feedback) {
    return new MailboxSentFeedbackListResponse.Item(
        feedback.getId(),
        feedback.getFeedbackType(),
        feedback.getFeedbackType().getDisplayTitle(),
        feedback.getContentText(),
        feedback.getProcessingStatus(),
        feedback.getCreatedAt());
  }

  private static MailboxSentFeedbackDetailResponse.Reply toSentFeedbackReply(MailboxLetter letter) {
    return new MailboxSentFeedbackDetailResponse.Reply(
        letter.getId(), letter.getTitle(), letter.getBodyText(), letter.getPublishedAt());
  }

  private List<MailboxSentFeedbackDetailResponse.Reply> getPublishedReplies(
      Long feedbackId, Long userProfileId) {
    List<MailboxLetterRecipient> replyRecipients =
        mailboxLetterRecipientRepository
            .findByRepresentativeFeedbackIdAndUserProfileIdOrderByCreatedAtAscIdAsc(
                feedbackId, userProfileId);
    if (replyRecipients.isEmpty()) {
      return List.of();
    }
    Map<Long, MailboxLetter> publishedRepliesById =
        mailboxLetterRepository
            .findAllById(replyRecipients.stream().map(MailboxLetterRecipient::getLetterId).toList())
            .stream()
            .filter(letter -> letter.getLetterType() == MailboxLetterType.REPLY)
            .filter(letter -> letter.getPublicationStatus() == MailboxPublicationStatus.PUBLISHED)
            .collect(Collectors.toMap(MailboxLetter::getId, Function.identity()));
    // findAllById의 조회 순서는 보장되지 않으므로 수신 정보 순서로 다시 조립한다.
    return replyRecipients.stream()
        .map(recipient -> publishedRepliesById.get(recipient.getLetterId()))
        .filter(Objects::nonNull)
        .map(MailboxFeedbackService::toSentFeedbackReply)
        .toList();
  }

  private static void validatePageSize(int size) {
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
  }

  private static String encodeFeedbackCursor(MailboxFeedback feedback) {
    // 커서 형식: feedback|createdAt|feedbackId.
    return encodeCursor("feedback|" + feedback.getCreatedAt() + "|" + feedback.getId());
  }

  private static FeedbackCursor decodeFeedbackCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      // encodeFeedbackCursor와 같은 필드 순서만 허용한다.
      String[] parts = decodeCursor(cursor);
      if (parts.length != 3 || !"feedback".equals(parts[0])) {
        throw new IllegalArgumentException();
      }
      return new FeedbackCursor(LocalDateTime.parse(parts[1]), Long.parseLong(parts[2]));
    } catch (IllegalArgumentException | DateTimeParseException exception) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
  }

  private static String encodeCursor(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String[] decodeCursor(String cursor) {
    return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
        .split("\\|", -1);
  }

  private record FeedbackCursor(LocalDateTime createdAt, Long feedbackId) {}
}
