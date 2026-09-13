// 사용자가 받은 편지 조회와 읽음 처리를 담당한다.

package com.landit.landitbe.feature.mailbox.letter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.mailbox.dto.MailboxUnreadCountResponse;
import com.landit.landitbe.feature.mailbox.feedback.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.feedback.repository.MailboxFeedbackRepository;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterRecipient;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.letter.dto.MailboxReceivedDetailResponse;
import com.landit.landitbe.feature.mailbox.letter.dto.MailboxReceivedListResponse;
import com.landit.landitbe.feature.mailbox.letter.repository.MailboxLetterReadRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.MailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.MailboxLetterRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.MailboxLetterRepository.ReceivedLetterSummary;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자가 받은 편지 조회와 읽음 처리를 담당한다. */
@Service
@RequiredArgsConstructor
public class MailboxLetterService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<Object>> CONTENT_BLOCKS_TYPE = new TypeReference<>() {};
  private static final int MAX_PAGE_SIZE = 100;
  private static final int FIRST_PAGE_CURSOR_PINNED = 2;
  private static final LocalDateTime FIRST_PAGE_CURSOR_SENT_AT =
      LocalDateTime.of(9999, 12, 31, 23, 59, 59);

  private final MailboxFeedbackRepository mailboxFeedbackRepository;
  private final MailboxLetterRepository mailboxLetterRepository;
  private final MailboxLetterRecipientRepository mailboxLetterRecipientRepository;
  private final MailboxLetterReadRepository mailboxLetterReadRepository;

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
        receivedLetters.stream().map(MailboxLetterService::toReceivedItem).toList(),
        nextCursor,
        hasNext);
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
        letter.getUnread());
  }

  private static MailboxReceivedDetailResponse toReceivedDetail(
      MailboxLetter letter, LocalDateTime readAt, MailboxFeedback quotedFeedback) {
    return new MailboxReceivedDetailResponse(
        letter.getId(),
        letter.getLetterType(),
        letter.getTitle(),
        toContentBlocks(letter.getContentBlocks()),
        letter.getBodyText(),
        quotedFeedback == null ? null : quotedFeedback.getFeedbackType(),
        quotedFeedback == null ? null : quotedFeedback.getContentText(),
        letter.isPinned(),
        letter.getPublishedAt(),
        readAt);
  }

  private static List<Object> toContentBlocks(JsonNode contentBlocks) {
    return contentBlocks == null
        ? null
        : OBJECT_MAPPER.convertValue(contentBlocks, CONTENT_BLOCKS_TYPE);
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

  private static void validatePageSize(int size) {
    if (size < 1 || size > MAX_PAGE_SIZE) {
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

  private record ReceivedCursor(boolean pinned, LocalDateTime sentAt, Long letterId) {}
}
