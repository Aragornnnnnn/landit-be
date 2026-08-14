// 전역 편지의 사용자별 읽음 정보를 조회하고 저장하는 Repository다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetterRead;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 전역 편지의 사용자별 읽음 정보를 조회하고 저장하는 Repository다. */
public interface MailboxLetterReadRepository extends JpaRepository<MailboxLetterRead, Long> {

  /**
   * 전역 편지를 아직 읽지 않은 경우에만 읽음 정보를 저장한다.
   *
   * <p>동시 요청에서는 고유 키 충돌을 무시해 최초 읽음 정보만 보존한다.
   *
   * @param letterId 편지 ID
   * @param userProfileId 사용자 ID
   * @return 새로 저장했으면 1, 이미 존재하면 0
   */
  @Modifying
  @Query(
      value =
          """
          INSERT INTO mailbox_letter_read (letter_id, user_profile_id, read_at, created_at)
          VALUES (:letterId, :userProfileId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON CONFLICT DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(@Param("letterId") Long letterId, @Param("userProfileId") Long userProfileId);

  /**
   * 사용자의 전역 편지 읽음 정보를 조회한다.
   *
   * @param letterId 편지 ID
   * @param userProfileId 사용자 ID
   * @return 조건에 맞는 읽음 정보
   */
  Optional<MailboxLetterRead> findByLetterIdAndUserProfileId(Long letterId, Long userProfileId);
}
