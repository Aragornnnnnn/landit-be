// 프리톡 턴 교정의 준비·확정·조회 경계를 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMessageFeedback;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkMessageFeedbackRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** 프리톡 턴 교정의 준비·확정·조회 경계를 검증한다. */
class FreeTalkMessageFeedbackServiceTest {

  private final FreeTalkMessageFeedbackRepository repository =
      mock(FreeTalkMessageFeedbackRepository.class);
  private final ConversationMessageService conversationMessageService =
      mock(ConversationMessageService.class);
  private final FreeTalkMessageFeedbackService service =
      new FreeTalkMessageFeedbackService(repository, conversationMessageService);

  @DisplayName("교정 준비는 대화 기록 ID를 발화에서 읽어 준비 상태 행을 만든다.")
  @Test
  void preparesFeedbackRowWithHistoryIdReadFromMessage() {
    SessionHistoryMessageSnapshot message = mock(SessionHistoryMessageSnapshot.class);
    when(message.getSessionHistoryId()).thenReturn(3L);
    when(conversationMessageService.require(7L)).thenReturn(message);

    service.prepareCorrection(7L);

    ArgumentCaptor<FreeTalkMessageFeedback> saved =
        ArgumentCaptor.forClass(FreeTalkMessageFeedback.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getSessionHistoryMessageId()).isEqualTo(7L);
    assertThat(saved.getValue().getSessionHistoryId()).isEqualTo(3L);
    assertThat(saved.getValue().getProcessingStatus()).isEqualTo(ProcessingStatus.PREPARING);
  }

  @DisplayName("교정 도입 전에 실패로 채워진 발화를 확정하면 새 행을 만들지 않고 그 행을 준비 상태로 되돌린다.")
  @Test
  void restartsBackfilledFailedFeedbackInsteadOfInsertingDuplicate() {
    FreeTalkMessageFeedback backfilled = FreeTalkMessageFeedback.preparing(7L, 3L);
    ReflectionTestUtils.setField(backfilled, "processingStatus", ProcessingStatus.FAILED);
    ReflectionTestUtils.setField(backfilled, "reactedToPartner", Boolean.TRUE);
    when(repository.findBySessionHistoryMessageId(7L)).thenReturn(Optional.of(backfilled));

    service.prepareCorrection(7L);

    assertThat(backfilled.getProcessingStatus()).isEqualTo(ProcessingStatus.PREPARING);
    assertThat(backfilled.getReactedToPartner()).isNull();
    verify(repository, never()).save(any());
  }

  @DisplayName("이미 판정이 끝난 교정은 다시 준비해도 지우지 않는다.")
  @Test
  void keepsCompletedCorrectionWhenPreparedAgain() {
    FreeTalkMessageFeedback completed = FreeTalkMessageFeedback.preparing(7L, 3L);
    ReflectionTestUtils.setField(completed, "processingStatus", ProcessingStatus.COMPLETED);
    ReflectionTestUtils.setField(completed, "betterSentence", "I went.");
    when(repository.findBySessionHistoryMessageId(7L)).thenReturn(Optional.of(completed));

    service.prepareCorrection(7L);

    assertThat(completed.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    assertThat(completed.getBetterSentence()).isEqualTo("I went.");
    verify(repository, never()).save(any());
  }

  @DisplayName("교정 판정은 준비 상태 조건의 한 갱신으로 문장 값과 함께 반영한다.")
  @Test
  void completesCorrectionInOneConditionalUpdate() {
    service.completeIfPreparing(
        7L,
        FreeTalkTurnCorrection.completed(
            new FreeTalkTurnCorrection.Sentence(
                "I go.", "I went.", "과거예요.", FreeTalkMistakePattern.TENSE),
            false));

    verify(repository)
        .updateIfPreparing(
            7L,
            ProcessingStatus.COMPLETED,
            false,
            "I go.",
            "I went.",
            "과거예요.",
            FreeTalkMistakePattern.TENSE,
            ProcessingStatus.PREPARING);
  }

  @DisplayName("교정 실패는 문장 값 없이 준비 상태 조건으로 실패를 기록한다.")
  @Test
  void failsCorrectionWithoutSentence() {
    service.failIfPreparing(7L);

    verify(repository)
        .updateIfPreparing(
            7L, ProcessingStatus.FAILED, null, null, null, null, null, ProcessingStatus.PREPARING);
  }

  @DisplayName("대화 기록의 교정을 사용자 발화 ID로 묶어 돌려준다.")
  @Test
  void groupsCorrectionsByUserMessageId() {
    when(repository.findBySessionHistoryId(3L))
        .thenReturn(List.of(FreeTalkMessageFeedback.preparing(7L, 3L)));

    assertThat(service.findBySessionHistoryId(3L))
        .containsOnlyKeys(7L)
        .containsValue(new FreeTalkTurnCorrection(ProcessingStatus.PREPARING, null, null));
  }
}
