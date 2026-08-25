// 편지함 어드민 공지·업데이트를 조회하고 저장한다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 편지함 어드민 공지·업데이트를 조회하고 저장한다. */
public interface AdminMailboxLetterRepository extends JpaRepository<MailboxLetter, Long> {

  /**
   * 사용자와 대표 피드백 후보에 연결된 답장을 최신순으로 조회한다.
   *
   * @param userProfileId 피드백 사용자 ID
   * @param feedbackIds 답장 연결 대상 피드백 ID
   * @param pageable 조회 개수와 페이지 조건
   * @return 사용자와 피드백 후보에 연결된 답장 목록
   */
  @Query(
      """
      select letter
      from MailboxLetter letter, MailboxLetterRecipient recipient
      where recipient.letterId = letter.id
        and recipient.userProfileId = :userProfileId
        and recipient.representativeFeedbackId in :feedbackIds
        and letter.letterType = com.landit.landitbe.feature.mailbox.domain.MailboxLetterType.REPLY
      order by letter.publishedAt desc, letter.id desc
      """)
  List<MailboxLetter> findRepliesByFeedbackIds(
      @Param("userProfileId") Long userProfileId,
      @Param("feedbackIds") Collection<Long> feedbackIds,
      Pageable pageable);

  /**
   * 어드민 편지 목록을 조건으로 조회한다.
   *
   * @param type 편지 유형
   * @param publicationStatus 게시 상태
   * @param pinned 상단 고정 여부
   * @param pageable 페이지 조건
   * @return 조건에 맞는 편지 페이지
   */
  @Query(
      """
      select letter
      from MailboxLetter letter
      where letter.letterType <> com.landit.landitbe.feature.mailbox.domain.MailboxLetterType.REPLY
        and (:type is null or letter.letterType = :type)
        and (:publicationStatus is null or letter.publicationStatus = :publicationStatus)
        and (:pinned is null or letter.pinned = :pinned)
      order by letter.createdAt desc, letter.id desc
      """)
  Page<MailboxLetter> search(
      @Param("type") MailboxLetterType type,
      @Param("publicationStatus") MailboxPublicationStatus publicationStatus,
      @Param("pinned") Boolean pinned,
      Pageable pageable);

  /**
   * 편집할 편지를 쓰기 잠금으로 조회한다.
   *
   * @param id 편지 ID
   * @return 잠금을 획득한 편지
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select letter from MailboxLetter letter where letter.id = :id")
  Optional<MailboxLetter> findByIdForUpdate(@Param("id") Long id);
}
