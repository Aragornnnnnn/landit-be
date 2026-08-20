// 편지함 편지 Entity를 조회하고 저장하는 Repository다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 편지함 편지 Entity를 조회하고 저장하는 Repository다. */
public interface MailboxLetterRepository extends JpaRepository<MailboxLetter, Long> {

  /**
   * 게시 상태와 식별자로 편지를 조회한다.
   *
   * @param id 편지 ID
   * @param publicationStatus 게시 상태
   * @return 조건에 맞는 편지
   */
  Optional<MailboxLetter> findByIdAndPublicationStatus(
      Long id, MailboxPublicationStatus publicationStatus);

  /**
   * 전역 편지와 사용자 답장을 합쳐 받은 편지 커서 페이지를 조회한다.
   *
   * <p>고정 여부, 발송 시각, 편지 ID의 내림차순으로 정렬한다.
   *
   * @param userProfileId 사용자 ID
   * @param cursorPinned 고정 여부({@code 1}: 고정, {@code 0}: 일반). 첫 페이지는 둘보다 큰 값
   * @param cursorSentAt 커서의 발송 시각
   * @param cursorLetterId 커서의 편지 ID
   * @param limit 조회 개수
   * @return 받은 편지 요약 목록
   */
  @Query(
      value =
          """
          SELECT received.letter_id AS "letterId",
                 received.letter_type AS "letterType",
                 received.title AS "title",
                 received.preview_text AS "preview",
                 received.is_pinned AS "pinned",
                 received.published_at AS "sentAt",
                 received.unread AS "unread"
          FROM (
              SELECT letter.id AS letter_id, letter.letter_type, letter.title,
                     letter.preview_text, letter.is_pinned, letter.published_at,
                     letter.published_at >= profile.created_at
                       AND letter_read.read_at IS NULL AS unread
              FROM mailbox_letter letter
              JOIN user_profile profile ON profile.id = :userProfileId
              LEFT JOIN mailbox_letter_read letter_read
                ON letter_read.letter_id = letter.id
               AND letter_read.user_profile_id = :userProfileId
              WHERE letter.publication_status = 'PUBLISHED'
                AND letter.letter_type IN ('NOTICE', 'UPDATE')
              UNION ALL
              SELECT letter.id, letter.letter_type, letter.title,
                     letter.preview_text, letter.is_pinned, letter.published_at,
                     recipient.read_at IS NULL
              FROM mailbox_letter letter
              JOIN mailbox_letter_recipient recipient ON recipient.letter_id = letter.id
              WHERE letter.publication_status = 'PUBLISHED'
                AND letter.letter_type = 'REPLY'
                AND recipient.user_profile_id = :userProfileId
          ) received
          WHERE CASE WHEN received.is_pinned THEN 1 ELSE 0 END < :cursorPinned
             OR (CASE WHEN received.is_pinned THEN 1 ELSE 0 END = :cursorPinned
                 AND (received.published_at < :cursorSentAt
                   OR (received.published_at = :cursorSentAt
                       AND received.letter_id < :cursorLetterId)))
          ORDER BY received.is_pinned DESC, received.published_at DESC, received.letter_id DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<ReceivedLetterSummary> findReceivedLetters(
      @Param("userProfileId") Long userProfileId,
      @Param("cursorPinned") int cursorPinned,
      @Param("cursorSentAt") LocalDateTime cursorSentAt,
      @Param("cursorLetterId") Long cursorLetterId,
      @Param("limit") int limit);

  /**
   * 사용자가 읽지 않은 전역 편지와 답장의 합계를 조회한다.
   *
   * @param userProfileId 사용자 ID
   * @return 안 읽은 편지 개수
   */
  @Query(
      value =
          """
          SELECT
            (SELECT COUNT(*)
             FROM mailbox_letter letter
             JOIN user_profile profile ON profile.id = :userProfileId
             WHERE letter.publication_status = 'PUBLISHED'
               AND letter.letter_type IN ('NOTICE', 'UPDATE')
               AND letter.published_at >= profile.created_at
               AND NOT EXISTS (
                 SELECT 1 FROM mailbox_letter_read letter_read
                 WHERE letter_read.letter_id = letter.id
                   AND letter_read.user_profile_id = :userProfileId))
            +
            (SELECT COUNT(*)
             FROM mailbox_letter_recipient recipient
             JOIN mailbox_letter letter ON letter.id = recipient.letter_id
             WHERE recipient.user_profile_id = :userProfileId
               AND recipient.read_at IS NULL
               AND letter.publication_status = 'PUBLISHED'
               AND letter.letter_type = 'REPLY')
          """,
      nativeQuery = true)
  long countUnreadLetters(@Param("userProfileId") Long userProfileId);

  /** 받은 편지 목록 조회 결과다. */
  interface ReceivedLetterSummary {

    /**
     * 편지 ID를 반환한다.
     *
     * @return 편지 ID
     */
    Long getLetterId();

    /**
     * 편지 유형을 반환한다.
     *
     * @return 편지 유형
     */
    String getLetterType();

    /**
     * 편지 제목을 반환한다.
     *
     * @return 편지 제목
     */
    String getTitle();

    /**
     * 목록 미리보기를 반환한다.
     *
     * @return 목록 미리보기
     */
    String getPreview();

    /**
     * 상단 고정 여부를 반환한다.
     *
     * @return 상단 고정 여부
     */
    boolean getPinned();

    /**
     * 발송 시각을 반환한다.
     *
     * @return 발송 시각
     */
    LocalDateTime getSentAt();

    /**
     * 안 읽은 편지 여부를 반환한다.
     *
     * @return 안 읽은 편지이면 {@code true}
     */
    boolean getUnread();
  }
}
