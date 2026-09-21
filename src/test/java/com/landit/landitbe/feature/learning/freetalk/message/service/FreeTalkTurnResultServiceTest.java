// 같은 AI 응답으로 온 속마음과 턴 교정이 한 경계에서 확정되는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.message.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.service.FreeTalkMessageFeedbackService;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 프리톡 한 턴의 비동기 AI 판정 결과 저장 경계를 검증한다. */
class FreeTalkTurnResultServiceTest {

  private final ConversationMessageService conversationMessageService =
      mock(ConversationMessageService.class);
  private final FreeTalkMessageFeedbackService messageFeedbackService =
      mock(FreeTalkMessageFeedbackService.class);
  private final FreeTalkTurnResultService service =
      new FreeTalkTurnResultService(conversationMessageService, messageFeedbackService);

  @DisplayName("같은 응답의 속마음과 턴 교정을 함께 확정한다.")
  @Test
  void completesInnerThoughtAndCorrectionTogether() {
    FreeTalkTurnCorrection correction = FreeTalkTurnCorrection.completed(null, true);
    when(conversationMessageService.completeInnerThought(7L, "thought", InnerThoughtType.GOOD))
        .thenReturn(1);

    service.complete(7L, "thought", InnerThoughtType.GOOD, correction);

    verify(messageFeedbackService).completeFirstAttempt(7L, correction);
  }

  @DisplayName("속마음이 먼저 시간 초과로 실패 처리된 발화에 응답이 늦게 와도 교정은 반영을 시도한다.")
  @Test
  void stillAppliesCorrectionWhenInnerThoughtWasAlreadyProcessed() {
    FreeTalkTurnCorrection correction = FreeTalkTurnCorrection.completed(null, true);
    when(conversationMessageService.completeInnerThought(7L, "thought", InnerThoughtType.GOOD))
        .thenReturn(0);

    service.complete(7L, "thought", InnerThoughtType.GOOD, correction);

    verify(messageFeedbackService).completeFirstAttempt(7L, correction);
  }

  @DisplayName("AI 호출에 실패하면 속마음은 실패로 확정하고 턴 교정은 첫 시도만 끝내 다시 시도되게 한다.")
  @Test
  void failsInnerThoughtAndLeavesCorrectionToRetry() {
    service.fail(7L);

    verify(conversationMessageService).failInnerThought(7L);
    verify(messageFeedbackService).failFirstAttempt(7L);
  }
}
