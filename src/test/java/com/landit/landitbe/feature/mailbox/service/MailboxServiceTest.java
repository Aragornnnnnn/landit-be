// 편지함 목록 서비스가 PostgreSQL에 안전한 커서 값을 전달하는지 검증한다.

package com.landit.landitbe.feature.mailbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.mailbox.repository.MailboxFeedbackRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterReadRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.repository.MailboxLetterRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 편지함 목록 서비스의 첫 페이지 커서 처리를 검증한다. */
@ExtendWith(MockitoExtension.class)
class MailboxServiceTest {

  @Mock private MailboxFeedbackRepository mailboxFeedbackRepository;
  @Mock private MailboxLetterRepository mailboxLetterRepository;
  @Mock private MailboxLetterRecipientRepository mailboxLetterRecipientRepository;
  @Mock private MailboxLetterReadRepository mailboxLetterReadRepository;
  @InjectMocks private MailboxService mailboxService;

  /** 첫 페이지 조회도 PostgreSQL이 타입을 결정할 수 있도록 null이 아닌 커서를 전달한다. */
  @Test
  void passesNonNullCursorValuesForFirstReceivedPage() {
    when(mailboxLetterRepository.findReceivedLetters(
            anyLong(), anyInt(), nullable(LocalDateTime.class), anyLong(), anyInt()))
        .thenReturn(List.of());
    ArgumentCaptor<Integer> pinnedCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<LocalDateTime> sentAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<Long> letterIdCaptor = ArgumentCaptor.forClass(Long.class);

    mailboxService.getReceivedLetters(1L, null, 20);

    verify(mailboxLetterRepository)
        .findReceivedLetters(
            anyLong(),
            pinnedCaptor.capture(),
            sentAtCaptor.capture(),
            letterIdCaptor.capture(),
            anyInt());
    assertThat(pinnedCaptor.getValue()).isGreaterThan(1);
    assertThat(sentAtCaptor.getValue()).isAfter(LocalDateTime.now());
    assertThat(letterIdCaptor.getValue()).isEqualTo(Long.MAX_VALUE);
  }
}
