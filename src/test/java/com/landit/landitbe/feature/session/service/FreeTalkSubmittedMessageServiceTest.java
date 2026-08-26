// 프리톡 처리 예약의 잠금 소유권 검증을 확인한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.memory.MemoryProperties;
import com.landit.landitbe.feature.character.service.StreakService;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.session.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 프리톡 처리 예약의 잠금 소유권 검증을 확인한다. */
class FreeTalkSubmittedMessageServiceTest {

  private final UserProfileService userProfileService = mock(UserProfileService.class);
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
  private final StreakService streakService = mock(StreakService.class);
  private final FreeTalkSubmittedMessageService service = service(new MemoryProperties(false));

  private FreeTalkSubmittedMessageService service(MemoryProperties memoryProperties) {
    return new FreeTalkSubmittedMessageService(
        userProfileService,
        learningSessionRepository,
        freeTalkSessionRepository,
        freeTalkTopicRepository,
        sessionHistoryRepository,
        sessionHistoryMessageRepository,
        dailySpeakingUsageService,
        streakService,
        memoryProperties,
        Clock.systemUTC());
  }

  private final LearningSession learningSession = mock(LearningSession.class);
  private final FreeTalkSession freeTalkSession = mock(FreeTalkSession.class);
  private final SessionHistory history = mock(SessionHistory.class);
  private final SessionHistoryMessage userMessage = mock(SessionHistoryMessage.class);

  @Test
  void doesNotPrepareMemoryGenerationWhenWriteIsDisabledForTimeLimitCompletion() {
    stubSuccessfulFinalization("old-owner");
    FreeTalkSession session = realFinalizationSession("old-owner");

    service(new MemoryProperties(false)).finalizeTimeLimit(messageReservation(), closingResult());

    assertThat(session.getMemoryGenerationStatus()).isNull();
  }

  @Test
  void preparesMemoryGenerationWhenWriteIsEnabledForTimeLimitCompletion() {
    stubSuccessfulFinalization("old-owner");
    FreeTalkSession session = realFinalizationSession("old-owner");

    service(new MemoryProperties(true)).finalizeTimeLimit(messageReservation(), closingResult());

    assertThat(session.getMemoryGenerationStatus())
        .isEqualTo(com.landit.landitbe.feature.session.domain.MemoryGenerationStatus.PREPARING);
  }

  @Test
  void doesNotPrepareMemoryGenerationWhenWriteIsDisabledForUserConfirmedCompletion() {
    stubSuccessfulFinalization("decision-7");
    FreeTalkSession session = realFinalizationSession("decision-7");

    service(new MemoryProperties(false)).finalizeEnd(decisionReservation(), closingResult());

    assertThat(session.getMemoryGenerationStatus()).isNull();
  }

  @Test
  void preparesMemoryGenerationWhenWriteIsEnabledForUserConfirmedCompletion() {
    stubSuccessfulFinalization("decision-7");
    FreeTalkSession session = realFinalizationSession("decision-7");

    service(new MemoryProperties(true)).finalizeEnd(decisionReservation(), closingResult());

    assertThat(session.getMemoryGenerationStatus())
        .isEqualTo(com.landit.landitbe.feature.session.domain.MemoryGenerationStatus.PREPARING);
  }

  private FreeTalkSession realFinalizationSession(String processingClientMessageId) {
    FreeTalkSession session = FreeTalkSession.start(300L, null, FreeTalkStartMode.AI_FIRST);
    session.startProcessing(processingClientMessageId);
    when(freeTalkSessionRepository.findByLearningSessionIdForUpdate(300L))
        .thenReturn(Optional.of(session));
    return session;
  }

  private void stubSuccessfulFinalization(String processingClientMessageId) {
    when(freeTalkSession.getProcessingClientMessageId()).thenReturn(processingClientMessageId);
    when(freeTalkSession.getTitle()).thenReturn("주말");
    when(freeTalkSession.getStartMode()).thenReturn(FreeTalkStartMode.AI_FIRST);
    when(freeTalkSession.getId()).thenReturn(30L);
    when(freeTalkSession.getConversationStatus())
        .thenReturn(
            com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus.COMPLETED);
    when(learningSession.getId()).thenReturn(300L);
    when(learningSession.getUserProfileId()).thenReturn(1L);
    when(history.getId()).thenReturn(3L);
    when(userMessage.getId()).thenReturn(7L);
    when(userMessage.getTurnNumber()).thenReturn(1);
    when(userMessage.getMessageSequence()).thenReturn(1);
    when(userMessage.getRole()).thenReturn(ConversationSpeaker.USER);
    SessionHistoryMessage aiMessage = mock(SessionHistoryMessage.class);
    when(aiMessage.getId()).thenReturn(8L);
    when(aiMessage.getTurnNumber()).thenReturn(2);
    when(aiMessage.getMessageSequence()).thenReturn(2);
    when(aiMessage.getRole()).thenReturn(ConversationSpeaker.AI);
    when(aiMessage.getContent()).thenReturn("See you!");
    when(aiMessage.getTranslatedContent()).thenReturn("또 봐요!");
    when(aiMessage.getEmotion()).thenReturn(null);
    when(sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(3L))
        .thenReturn(List.of(userMessage));
    when(sessionHistoryMessageRepository.save(any(SessionHistoryMessage.class)))
        .thenReturn(aiMessage);
    when(sessionHistoryMessageRepository.countBySessionHistoryIdAndRole(any(Long.class), any()))
        .thenReturn(1L);
    when(dailySpeakingUsageService.usage(1L))
        .thenReturn(
            new FreeTalkDailySpeakingUsageService.DailySpeakingUsage(
                java.time.LocalDate.now(), 1_200L, 58_800L));
  }

  private AiFreeTalkClosingResult closingResult() {
    return new AiFreeTalkClosingResult("주말", "See you!", "또 봐요!", null);
  }

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
        1L,
        java.time.LocalDate.now(),
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
        1L,
        300L,
        3L,
        30L,
        "chloe",
        7L,
        FreeTalkExitDecision.END,
        true,
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
