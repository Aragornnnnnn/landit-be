// 사용자별 답장 전달 정보를 조회하고 저장하는 Repository다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetterRecipient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자별 답장 전달 정보를 조회하고 저장하는 Repository다. */
public interface MailboxLetterRecipientRepository
    extends JpaRepository<MailboxLetterRecipient, Long> {

  /**
   * 사용자가 받은 답장 전달 정보를 조회한다.
   *
   * @param letterId 답장 편지 ID
   * @param userProfileId 사용자 ID
   * @return 조건에 맞는 답장 전달 정보
   */
  Optional<MailboxLetterRecipient> findByLetterIdAndUserProfileId(
      Long letterId, Long userProfileId);

  /**
   * 답장을 아직 읽지 않은 경우에만 읽은 시각을 기록한다.
   *
   * <p>조건부 갱신으로 동시 상세 요청에서도 최초 읽은 시각을 보존한다.
   *
   * @param letterId 답장 편지 ID
   * @param userProfileId 사용자 ID
   * @return 새로 기록했으면 1, 이미 읽었거나 전달되지 않은 답장이면 0
   */
  @Modifying
  @Query(
      value =
          """
          UPDATE mailbox_letter_recipient
          SET read_at = CURRENT_TIMESTAMP
          WHERE letter_id = :letterId
            AND user_profile_id = :userProfileId
            AND read_at IS NULL
          """,
      nativeQuery = true)
  int markReadIfUnread(
      @Param("letterId") Long letterId, @Param("userProfileId") Long userProfileId);

  /**
   * 대표 피드백의 답장 전달 정보를 오래된 순서로 조회한다.
   *
   * @param representativeFeedbackId 대표 피드백 ID
   * @param userProfileId 사용자 ID
   * @return 답장 전달 정보 목록
   */
  List<MailboxLetterRecipient>
      findByRepresentativeFeedbackIdAndUserProfileIdOrderByCreatedAtAscIdAsc(
          Long representativeFeedbackId, Long userProfileId);
}
