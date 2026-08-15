// 편지함 어드민 공지·업데이트를 조회하고 저장한다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import jakarta.persistence.LockModeType;
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
