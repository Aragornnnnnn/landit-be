// 편지함 어드민 답장의 사용자별 수신 정보를 저장한다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetterRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

/** 편지함 어드민 답장의 사용자별 수신 정보를 저장한다. */
public interface AdminMailboxLetterRecipientRepository
    extends JpaRepository<MailboxLetterRecipient, Long> {}
