// 사용자 편지함 피드백 Entity를 조회하고 저장하는 Repository다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxFeedback;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자 편지함 피드백 Entity를 조회하고 저장하는 Repository다. */
public interface MailboxFeedbackRepository extends JpaRepository<MailboxFeedback, Long> {

  /**
   * 사용자 소유의 피드백을 식별자로 조회한다.
   *
   * @param id 피드백 ID
   * @param userProfileId 사용자 ID
   * @return 조건에 맞는 피드백
   */
  Optional<MailboxFeedback> findByIdAndUserProfileId(Long id, Long userProfileId);

  /**
   * 사용자의 피드백을 최신순으로 페이지 조회한다.
   *
   * @param userProfileId 사용자 ID
   * @param pageable 페이지 조건
   * @return 피드백 목록
   */
  List<MailboxFeedback> findByUserProfileIdOrderByCreatedAtDescIdDesc(
      Long userProfileId, Pageable pageable);

  /**
   * 커서 기준보다 오래된 사용자의 피드백을 최신순으로 페이지 조회한다.
   *
   * @param userProfileId 사용자 ID
   * @param createdAt 커서 생성 시각
   * @param feedbackId 커서 피드백 ID
   * @param pageable 페이지 조건
   * @return 피드백 목록
   */
  @Query(
      """
      select feedback
      from MailboxFeedback feedback
      where feedback.userProfileId = :userProfileId
        and (feedback.createdAt < :createdAt
             or (feedback.createdAt = :createdAt and feedback.id < :feedbackId))
      order by feedback.createdAt desc, feedback.id desc
      """)
  List<MailboxFeedback> findBeforeCursor(
      @Param("userProfileId") Long userProfileId,
      @Param("createdAt") LocalDateTime createdAt,
      @Param("feedbackId") Long feedbackId,
      Pageable pageable);
}
