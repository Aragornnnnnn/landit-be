// ConversationMessageService의 메시지 조회와 상태 변경 위임을 단위 테스트한다.

package com.landit.landitbe.feature.learning.conversation.history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.conversation.exception.SessionException;
import com.landit.landitbe.feature.learning.conversation.history.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.history.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ConversationMessageService의 메시지 조회와 상태 변경 위임을 단위 테스트한다. */
class ConversationMessageServiceTest {

  private final SessionHistoryMessageRepository repository =
      mock(SessionHistoryMessageRepository.class);
  private final ConversationMessageService service = new ConversationMessageService(repository);

  /** 메시지 조회 결과는 영속 엔티티와 분리된 값을 반환한다. */
  @DisplayName("메시지 조회 결과는 영속 엔티티와 분리된 값을 반환한다.")
  @Test
  void returnsMessageById() {
    SessionHistoryMessage message = mock(SessionHistoryMessage.class);
    when(message.getId()).thenReturn(3L);
    when(message.getContent()).thenReturn("original");
    when(repository.findById(3L)).thenReturn(Optional.of(message));

    SessionHistoryMessageSnapshot snapshot = service.require(3L);
    when(message.getContent()).thenReturn("changed");

    assertThat(snapshot.id()).isEqualTo(3L);
    assertThat(snapshot.content()).isEqualTo("original");
  }

  /** 세션 히스토리에 속하지 않은 메시지는 세션 기능 예외로 변환한다. */
  @DisplayName("세션 히스토리에 속하지 않은 메시지는 세션 기능 예외로 변환한다.")
  @Test
  void rejectsMessageOutsideSessionHistory() {
    when(repository.findByIdAndSessionHistoryId(3L, 7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requireInHistory(3L, 7L))
        .isInstanceOf(SessionException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.RESOURCE_NOT_FOUND);
  }

  /** 속마음 완료 상태 변경에 고정된 상태 조건을 전달한다. */
  @DisplayName("속마음 완료 상태 변경에 고정된 상태 조건을 전달한다.")
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

  /** 프리톡 속마음과 턴 교정을 준비 상태 조건의 한 갱신으로 전달한다. */
  @DisplayName("프리톡 속마음과 턴 교정을 준비 상태 조건의 한 갱신으로 전달한다.")
  @Test
  void completesFreeTalkInnerThoughtAndCorrectionInOneUpdate() {
    service.completeFreeTalkInnerThought(
        3L,
        "thought",
        InnerThoughtType.GOOD,
        FreeTalkTurnCorrection.completed(
            new FreeTalkTurnCorrection.Sentence(
                "I go.", "I went.", "과거예요.", FreeTalkMistakePattern.TENSE),
            false));

    verify(repository)
        .completeInnerThoughtAndCorrectionIfPreparing(
            3L,
            "thought",
            InnerThoughtType.GOOD,
            "I go.",
            "I went.",
            "과거예요.",
            FreeTalkMistakePattern.TENSE,
            false,
            ProcessingStatus.COMPLETED,
            ProcessingStatus.COMPLETED,
            ProcessingStatus.PREPARING);
  }

  /** 교정 판정에 실패해도 속마음은 완료로 저장하고 교정만 실패로 남긴다. */
  @DisplayName("교정 판정에 실패해도 속마음은 완료로 저장하고 교정만 실패로 남긴다.")
  @Test
  void completesInnerThoughtWhileMarkingCorrectionFailed() {
    service.completeFreeTalkInnerThought(
        3L, "thought", InnerThoughtType.GOOD, FreeTalkTurnCorrection.failed());

    verify(repository)
        .completeInnerThoughtAndCorrectionIfPreparing(
            3L,
            "thought",
            InnerThoughtType.GOOD,
            null,
            null,
            null,
            null,
            null,
            ProcessingStatus.FAILED,
            ProcessingStatus.COMPLETED,
            ProcessingStatus.PREPARING);
  }
}
