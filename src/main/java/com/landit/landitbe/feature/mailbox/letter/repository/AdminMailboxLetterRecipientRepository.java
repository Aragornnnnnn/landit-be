// 편지함 답장과 직접 편지의 사용자별 수신 정보를 조회하고 저장한다.

package com.landit.landitbe.feature.mailbox.letter.repository;

import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterRecipient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 편지함 답장과 직접 편지의 사용자별 수신 정보를 조회하고 저장한다. */
public interface AdminMailboxLetterRecipientRepository
    extends JpaRepository<MailboxLetterRecipient, Long> {

  /**
   * 탈퇴 여부와 관계없이 편지의 수신 이력을 사용자 ID 순으로 조회한다.
   *
   * @param letterId 편지 ID
   * @return 수신자별 최초 읽음 정보
   */
  List<MailboxLetterRecipient> findByLetterIdOrderByUserProfileIdAsc(Long letterId);
}
