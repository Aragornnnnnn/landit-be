// 커밋된 평가 예약이 실행되지 못해도 조회에서 fallback으로 끝나는지 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.domain.CompletionReason;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.repository.UserLevelAssessmentRepository;
import com.landit.landitbe.shared.domain.Locale;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class SessionLevelAssessmentRecoveryTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void expiryWithoutPollingDiscardsQueuedOrLateModelResult(boolean expiresInQueue) {
    final var sessions = mock(LearningSessionService.class);
    final var profiles = mock(UserProfileService.class);
    final var contexts = mock(SessionFeedbackContextService.class);
    final var evaluator = mock(SessionLevelAssessmentService.class);
    final var repository = mock(UserLevelAssessmentRepository.class);
    final var ai = mock(AiConversationClient.class);
    var transactions = mock(PlatformTransactionManager.class);
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    var clock = mock(Clock.class);
    Instant start = Instant.parse("2026-07-01T00:00:00Z");
    when(clock.instant()).thenReturn(start);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    var session =
        LearningSession.startScenario(1L, 1L, Locale.EN, Locale.KR, LocalDateTime.now(clock));
    session.completeBySystem(CompletionReason.MAX_TURNS_REACHED, LocalDateTime.now(clock));
    session.prepareLevelAssessment(LocalDateTime.now(clock));
    var context = mock(LoadedSessionFeedbackContext.class);
    when(context.sessionId()).thenReturn(10L);
    when(contexts.load(1L, 10L)).thenReturn(context);
    when(sessions.findOwned(1L, 10L)).thenReturn(session);
    when(sessions.findOwnedCompletedForUpdate(1L, 10L)).thenReturn(session);
    when(sessions.isLatestCompletedScenario(session)).thenReturn(true);
    var queued = new java.util.concurrent.atomic.AtomicReference<Runnable>();
    var service =
        new SessionLevelAssessmentGenerationService(
            sessions,
            profiles,
            contexts,
            evaluator,
            repository,
            ai,
            transactions,
            queued::set,
            clock,
            new SessionLevelAssessmentLaunchService(
                new com.landit.landitbe.feature.subscription.service
                    .SubscriptionLaunchPolicyService(
                    new SubscriptionProperties("2026-06-01T00:00:00Z"), clock),
                clock));
    service.startIfNeeded(1L, 10L);
    if (expiresInQueue) {
      when(clock.instant()).thenReturn(start.plusSeconds(121));
    } else {
      when(ai.generateSessionLevelAssessment(any()))
          .thenAnswer(
              invocation -> {
                when(clock.instant()).thenReturn(start.plusSeconds(121));
                return mock(
                    com.landit.landitbe.feature.session.client.ai.AiSessionLevelAssessment.class);
              });
    }
    queued.get().run();
    verify(evaluator).assessApplyAndSave(eq(1L), eq(context), isNull(), eq(true), any());
    assertThat(session.getLevelAssessmentProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    if (expiresInQueue) {
      verifyNoInteractions(ai);
    } else {
      verify(ai).generateSessionLevelAssessment(any());
    }
  }

  @Test
  void contextFailureLeavesReservationRecoverableWithoutAnotherAiCall() {
    final var sessions = mock(LearningSessionService.class);
    final var profiles = mock(UserProfileService.class);
    final var contexts = mock(SessionFeedbackContextService.class);
    final var evaluator = mock(SessionLevelAssessmentService.class);
    final var repository = mock(UserLevelAssessmentRepository.class);
    final var ai = mock(AiConversationClient.class);
    var transactions = mock(PlatformTransactionManager.class);
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    var clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
    var session =
        LearningSession.startScenario(1L, 1L, Locale.EN, Locale.KR, LocalDateTime.now(clock));
    session.completeBySystem(CompletionReason.MAX_TURNS_REACHED, LocalDateTime.now(clock));
    session.prepareLevelAssessment(LocalDateTime.now(clock).minusMinutes(3));
    var context = mock(LoadedSessionFeedbackContext.class);
    when(context.sessionId()).thenReturn(10L);
    when(contexts.load(1L, 10L))
        .thenThrow(new IllegalStateException("temporary failure"))
        .thenReturn(context);
    when(sessions.findOwned(1L, 10L)).thenReturn(session);
    when(sessions.findOwnedCompletedForUpdate(1L, 10L)).thenReturn(session);
    when(repository.findByLearningSessionId(10L)).thenReturn(Optional.empty());
    var service =
        new SessionLevelAssessmentGenerationService(
            sessions,
            profiles,
            contexts,
            evaluator,
            repository,
            ai,
            transactions,
            Runnable::run,
            clock,
            new SessionLevelAssessmentLaunchService(
                new com.landit.landitbe.feature.subscription.service
                    .SubscriptionLaunchPolicyService(
                    new SubscriptionProperties("2026-06-01T00:00:00Z"), clock),
                clock));

    service.startIfNeeded(1L, 10L);
    assertThat(session.getLevelAssessmentProcessingStatus()).isEqualTo(ProcessingStatus.PREPARING);
    service.get(1L, 10L);

    verify(evaluator).assessApplyAndSave(eq(1L), eq(context), isNull(), eq(false), any());
    assertThat(session.getLevelAssessmentProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    verifyNoInteractions(ai);
  }
}
