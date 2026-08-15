// 프리톡 처리 예약의 잠금 소유권 검증을 확인한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.session.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 프리톡 처리 예약의 잠금 소유권 검증을 확인한다. */
class FreeTalkSubmittedMessageServiceTest {

  private final LearningSessionRepository learningSessionRepository =
      mock(LearningSessionRepository.class);
  private final FreeTalkSessionRepository freeTalkSessionRepository =
      mock(FreeTalkSessionRepository.class);
  private final FreeTalkTopicRepository freeTalkTopicRepository =
      mock(FreeTalkTopicRepository.class);
  private final SessionHistoryRepository sessionHistoryRepository =
      mock(SessionHistoryRepository.class);
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository =
      mock(SessionHistoryMessageRepository.class);
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService =
      mock(FreeTalkDailySpeakingUsageService.class);
  private final FreeTalkSubmittedMessageService service =
      new FreeTalkSubmittedMessageService(
          learningSessionRepository,
          freeTalkSessionRepository,
          freeTalkTopicRepository,
          sessionHistoryRepository,
          sessionHistoryMessageRepository,
          dailySpeakingUsageService);
  private final LearningSession learningSession = mock(LearningSession.class);
  private final FreeTalkSession freeTalkSession = mock(FreeTalkSession.class);
  private final SessionHistory history = mock(SessionHistory.class);
  private final SessionHistoryMessage userMessage = mock(SessionHistoryMessage.class);

  @BeforeEach
  void setUp() {
    when(learningSessionRepository.findById(300L)).thenReturn(Optional.of(learningSession));
    when(freeTalkSessionRepository.findByLearningSessionIdForUpdate(300L))
        .thenReturn(Optional.of(freeTalkSession));
    when(sessionHistoryRepository.findById(3L)).thenReturn(Optional.of(history));
    when(sessionHistoryMessageRepository.findById(7L)).thenReturn(Optional.of(userMessage));
    when(freeTalkSession.getProcessingClientMessageId()).thenReturn("new-owner");
  }

  @Test
  void rejectsTurnFinalizationWhenAnotherRequestOwnsProcessingLock() {
    assertConflict(() -> service.finalizeTurn(messageReservation(), null));
    verify(freeTalkSession, never()).clearProcessing();
  }

  @Test
  void rejectsTimeLimitFinalizationWhenAnotherRequestOwnsProcessingLock() {
    assertConflict(() -> service.finalizeTimeLimit(messageReservation(), null));
    verify(freeTalkSession, never()).clearProcessing();
  }

  @Test
  void rejectsContinueFinalizationWhenAnotherRequestOwnsProcessingLock() {
    assertConflict(() -> service.finalizeContinue(decisionReservation(), null));
    verify(freeTalkSession, never()).clearProcessing();
  }

  @Test
  void rejectsEndFinalizationWhenAnotherRequestOwnsProcessingLock() {
    assertConflict(() -> service.finalizeEnd(decisionReservation(), null));
    verify(freeTalkSession, never()).clearProcessing();
  }

  @Test
  void doesNotClearAnotherDecisionProcessingLockDuringCompensation() {
    service.compensateDecision(decisionReservation());

    verify(freeTalkSession, never()).clearProcessing();
  }

  private FreeTalkSubmittedMessageService.Reservation messageReservation() {
    return new FreeTalkSubmittedMessageService.Reservation(
        300L,
        30L,
        "chloe",
        3L,
        7L,
        "old-owner",
        1200L,
        false,
        false,
        "EN",
        "KO",
        new AiFreeTalkTopic(null, "주말", null),
        List.of(new AiConversationHistoryMessage(7L, 1, "USER", "Hello.", null)));
  }

  private FreeTalkSubmittedMessageService.DecisionReservation decisionReservation() {
    return new FreeTalkSubmittedMessageService.DecisionReservation(
        300L,
        3L,
        30L,
        "chloe",
        7L,
        FreeTalkExitDecision.END,
        "EN",
        "KO",
        new AiFreeTalkTopic(null, "주말", null),
        List.of(new AiConversationHistoryMessage(7L, 1, "USER", "Hello.", null)));
  }

  private void assertConflict(Runnable invocation) {
    assertThatThrownBy(invocation::run)
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
  }
}
