// SessionMessageService의 메시지 조회와 상태 변경 위임을 단위 테스트한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** SessionMessageService의 메시지 조회와 상태 변경 위임을 단위 테스트한다. */
class SessionMessageServiceTest {

  private final SessionHistoryMessageRepository repository =
      mock(SessionHistoryMessageRepository.class);
  private final SessionMessageService service = new SessionMessageService(repository);

  /** 메시지 ID 조회 결과를 반환한다. */
  @Test
  void returnsMessageById() {
    SessionHistoryMessage message = mock(SessionHistoryMessage.class);
    when(repository.findById(3L)).thenReturn(Optional.of(message));

    assertThat(service.require(3L)).isSameAs(message);
  }

  /** 세션 히스토리에 속하지 않은 메시지는 세션 기능 예외로 변환한다. */
  @Test
  void rejectsMessageOutsideSessionHistory() {
    when(repository.findByIdAndSessionHistoryId(3L, 7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requireInHistory(3L, 7L))
        .isInstanceOf(SessionException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.RESOURCE_NOT_FOUND);
  }

  /** 속마음 완료 상태 변경에 고정된 상태 조건을 전달한다. */
  @Test
  void completesInnerThoughtOnlyWhilePreparing() {
    service.completeInnerThought(3L, "thought", InnerThoughtType.GOOD);

    verify(repository)
        .completeInnerThoughtIfPreparing(
            3L,
            "thought",
            InnerThoughtType.GOOD,
            ProcessingStatus.COMPLETED,
            ProcessingStatus.PREPARING);
  }
}
