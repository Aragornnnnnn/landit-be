// 편지함 사용자 기능의 피드백 등록, 편지 조회와 읽음 처리를 담당한다.

package com.landit.landitbe.feature.mailbox.service;

import com.landit.landitbe.feature.mailbox.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterRecipient;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.dto.MailboxFeedbackSubmitRequest;
import com.landit.landitbe.feature.mailbox.dto.MailboxReceivedDetailResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxReceivedListResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxSentFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxSentFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxUnreadCountResponse;
import com.landit.landitbe.feature.mailbox.repository.MailboxFeedbackRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterReadRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterRepository.ReceivedLetterSummary;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 편지함 피드백 등록과 편지 조회·읽음 처리를 담당한다. */
@Service
public class MailboxService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final int FIRST_PAGE_CURSOR_PINNED = 2;
  private static final LocalDateTime FIRST_PAGE_CURSOR_SENT_AT =
      LocalDateTime.of(9999, 12, 31, 23, 59, 59);

  private final MailboxFeedbackRepository mailboxFeedbackRepository;
  private final MailboxLetterRepository mailboxLetterRepository;
  private final MailboxLetterRecipientRepository mailboxLetterRecipientRepository;
  private final MailboxLetterReadRepository mailboxLetterReadRepository;

  /**
   * 편지함 사용자 기능에 필요한 저장소를 주입받는다.
   *
   * @param mailboxFeedbackRepository 피드백 저장소
   * @param mailboxLetterRepository 편지 저장소
   * @param mailboxLetterRecipientRepository 답장 수신 정보 저장소
   * @param mailboxLetterReadRepository 전역 편지 읽음 저장소
   */
  public MailboxService(
      MailboxFeedbackRepository mailboxFeedbackRepository,
      MailboxLetterRepository mailboxLetterRepository,
      MailboxLetterRecipientRepository mailboxLetterRecipientRepository,
      MailboxLetterReadRepository mailboxLetterReadRepository) {
    this.mailboxFeedbackRepository = mailboxFeedbackRepository;
    this.mailboxLetterRepository = mailboxLetterRepository;
    this.mailboxLetterRecipientRepository = mailboxLetterRecipientRepository;
    this.mailboxLetterReadRepository = mailboxLetterReadRepository;
  }

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
        feedbacks.stream().map(MailboxService::toSentFeedbackItem).toList(), nextCursor, hasNext);
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
        getPublishedReplies(feedback.getId(), userProfileId));
  }

  /**
   * 사용자의 전역 편지와 답장을 최신순으로 조회한다.
   *
   * @param userProfileId 사용자 ID
   * @param cursor 다음 페이지 커서
   * @param size 페이지 크기
   * @return 받은 편지 커서 페이지
   * @throws ApiException 커서나 페이지 크기가 유효하지 않은 경우
   */
  @Transactional(readOnly = true)
  public MailboxReceivedListResponse getReceivedLetters(
      Long userProfileId, String cursor, int size) {
    validatePageSize(size);
    ReceivedCursor receivedCursor = decodeReceivedCursor(cursor);
    int cursorPinned =
        receivedCursor == null ? FIRST_PAGE_CURSOR_PINNED : receivedCursor.pinned() ? 1 : 0;
    LocalDateTime cursorSentAt =
        receivedCursor == null ? FIRST_PAGE_CURSOR_SENT_AT : receivedCursor.sentAt();
    long cursorLetterId = receivedCursor == null ? Long.MAX_VALUE : receivedCursor.letterId();
    List<ReceivedLetterSummary> receivedLetters =
        mailboxLetterRepository.findReceivedLetters(
            userProfileId, cursorPinned, cursorSentAt, cursorLetterId, size + 1);
    boolean hasNext = receivedLetters.size() > size;
    if (hasNext) {
      receivedLetters = receivedLetters.subList(0, size);
    }
    String nextCursor = hasNext ? encodeReceivedCursor(receivedLetters.getLast()) : null;
    return new MailboxReceivedListResponse(
        receivedLetters.stream().map(MailboxService::toReceivedItem).toList(), nextCursor, hasNext);
  }

  /**
   * 사용자가 볼 수 있는 편지의 상세를 조회하고 읽음 상태를 기록한다.
   *
   * @param userProfileId 사용자 ID
   * @param letterId 편지 ID
   * @return 받은 편지 상세
   * @throws ApiException 편지가 없거나 사용자에게 전달되지 않은 경우
   */
  @Transactional
  public MailboxReceivedDetailResponse getReceivedLetter(Long userProfileId, Long letterId) {
    MailboxLetter letter = findPublishedLetter(letterId);
    LocalDateTime readAt;
    MailboxFeedback quotedFeedback = null;
    if (letter.getLetterType() == MailboxLetterType.REPLY) {
      // 답장은 사용자별 수신 정보에 최초 읽은 시각을 직접 기록한다.
      mailboxLetterRecipientRepository.markReadIfUnread(letterId, userProfileId);
      MailboxLetterRecipient recipient =
          mailboxLetterRecipientRepository
              .findByLetterIdAndUserProfileId(letterId, userProfileId)
              .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
      readAt = recipient.getReadAt();
      quotedFeedback =
          mailboxFeedbackRepository
              .findByIdAndUserProfileId(recipient.getRepresentativeFeedbackId(), userProfileId)
              .orElseThrow(() -> new IllegalStateException("답장과 연결된 대표 피드백을 찾을 수 없습니다."));
    } else {
      // 전역 편지는 사용자마다 읽음 행을 한 번만 생성한다.
      mailboxLetterReadRepository.insertIfAbsent(letterId, userProfileId);
      readAt =
          mailboxLetterReadRepository
              .findByLetterIdAndUserProfileId(letterId, userProfileId)
              .orElseThrow(() -> new IllegalStateException("전역 편지 읽음 정보 저장 후 조회에 실패했습니다."))
              .getReadAt();
    }
    return toReceivedDetail(letter, readAt, quotedFeedback);
  }

  /**
   * 사용자가 볼 수 있는 편지 중 읽지 않은 개수를 반환한다.
   *
   * @param userProfileId 사용자 ID
   * @return 안 읽은 편지 개수
   */
  @Transactional(readOnly = true)
  public MailboxUnreadCountResponse getUnreadCount(Long userProfileId) {
    long unreadCount = mailboxLetterRepository.countUnreadLetters(userProfileId);
    return new MailboxUnreadCountResponse(unreadCount);
  }

  private MailboxLetter findPublishedLetter(Long letterId) {
    return mailboxLetterRepository
        .findByIdAndPublicationStatus(letterId, MailboxPublicationStatus.PUBLISHED)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private static MailboxReceivedListResponse.Item toReceivedItem(ReceivedLetterSummary letter) {
    return new MailboxReceivedListResponse.Item(
        letter.getLetterId(),
        MailboxLetterType.valueOf(letter.getLetterType()),
        letter.getTitle(),
        letter.getPreview(),
        letter.getPinned(),
        letter.getSentAt(),
        letter.getReadAt() == null);
  }

  private static MailboxReceivedDetailResponse toReceivedDetail(
      MailboxLetter letter, LocalDateTime readAt, MailboxFeedback quotedFeedback) {
    return new MailboxReceivedDetailResponse(
        letter.getId(),
        letter.getLetterType(),
        letter.getTitle(),
        letter.getContentBlocks(),
        letter.getBodyText(),
        quotedFeedback == null ? null : quotedFeedback.getFeedbackType(),
        quotedFeedback == null ? null : quotedFeedback.getContentText(),
        letter.isPinned(),
        letter.getPublishedAt(),
        readAt);
  }

  private static String encodeReceivedCursor(ReceivedLetterSummary letter) {
    // 커서 형식: received|pinned(0 또는 1)|sentAt|letterId.
    return encodeCursor(
        "received|"
            + (letter.getPinned() ? "1" : "0")
            + "|"
            + letter.getSentAt()
            + "|"
            + letter.getLetterId());
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
        .map(MailboxService::toSentFeedbackReply)
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

  private static ReceivedCursor decodeReceivedCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      // encodeReceivedCursor와 같은 필드 순서와 고정 여부 값만 허용한다.
      String[] parts = decodeCursor(cursor);
      if (parts.length != 4
          || !"received".equals(parts[0])
          || (!"0".equals(parts[1]) && !"1".equals(parts[1]))) {
        throw new IllegalArgumentException();
      }
      return new ReceivedCursor(
          "1".equals(parts[1]), LocalDateTime.parse(parts[2]), Long.parseLong(parts[3]));
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

  private record ReceivedCursor(boolean pinned, LocalDateTime sentAt, Long letterId) {}
}
