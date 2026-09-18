// 커밋된 평가 예약이 실행되지 못해도 조회에서 fallback으로 끝나는지 검증한다.

package com.landit.landitbe.feature.learning.scenario.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.learning.conversation.domain.CompletionReason;
import com.landit.landitbe.feature.learning.conversation.domain.LearningSession;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.scenario.assessment.repository.UserLevelAssessmentRepository;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.LoadedSessionFeedbackContext;
import com.landit.landitbe.feature.learning.scenario.feedback.service.SessionFeedbackContextService;
import com.landit.landitbe.feature.learning.scenario.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.Locale;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class SessionLevelAssessmentRecoveryTest {
  private final LearningSessionService sessions = mock(LearningSessionService.class);
  private final UserProfileService profiles = mock(UserProfileService.class);
  private final SessionFeedbackContextService contexts = mock(SessionFeedbackContextService.class);
  private final SessionLevelAssessmentService evaluator = mock(SessionLevelAssessmentService.class);
  private final UserLevelAssessmentRepository repository =
      mock(UserLevelAssessmentRepository.class);
  private final AiConversationClient ai = mock(AiConversationClient.class);
  private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);

  @DisplayName("도입 시각 전에는 진행 중 세션의 수준 평가 조회에 null을 반환하고 작업을 생략한다.")
  @Test
  void futureLaunchReturnsNullForInProgressSessionAndSkipsAssessmentWork() {
    final var executor = mock(org.springframework.core.task.TaskExecutor.class);
    var clock = mock(Clock.class);
    Instant launchInstant = Instant.parse("2026-07-01T00:00:00Z");
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(clock.instant()).thenReturn(launchInstant.minusSeconds(1));
    var session =
        LearningSession.startScenario(1L, 1L, Locale.EN, Locale.KR, LocalDateTime.now(clock));
    org.springframework.test.util.ReflectionTestUtils.setField(session, "id", 10L);
    when(sessions.findOwned(1L, 10L))
        .thenAnswer(invocation -> LearningSessionSnapshot.from(session));
    var service = assessmentService(clock, executor, "2026-07-01T09:00:00+09:00");

    service.startIfNeeded(1L, 10L);
    verifyNoInteractions(sessions);
    assertThat(service.get(1L, 10L)).isNull();
    verify(sessions).findOwned(1L, 10L);
    verifyNoInteractions(profiles, contexts, evaluator, repository, ai, transactions, executor);

    when(clock.instant()).thenReturn(launchInstant);
    assertThat(service.get(1L, 10L)).isNotNull();
    verifyNoInteractions(profiles, contexts, evaluator, ai, transactions, executor);
  }

  @DisplayName("대기 중 또는 AI 호출 중 평가 기한이 지나면 모델 결과를 버리고 대체 평가를 저장한다.")
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void expiryWithoutPollingDiscardsQueuedOrLateModelResult(boolean expiresInQueue) {
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    var clock = mock(Clock.class);
    Instant start = Instant.parse("2026-07-01T00:00:00Z");
    when(clock.instant()).thenReturn(start);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    var session =
        LearningSession.startScenario(1L, 1L, Locale.EN, Locale.KR, LocalDateTime.now(clock));
    org.springframework.test.util.ReflectionTestUtils.setField(session, "id", 10L);
    session.completeBySystem(CompletionReason.MAX_TURNS_REACHED, LocalDateTime.now(clock));
    session.prepareLevelAssessment(LocalDateTime.now(clock));
    when(sessions.completeLevelAssessment(10L))
        .thenAnswer(
            invocation -> {
              session.completeLevelAssessment();
              return LearningSessionSnapshot.from(session);
            });
    var context = mock(LoadedSessionFeedbackContext.class);
    when(context.sessionId()).thenReturn(10L);
    when(contexts.load(1L, 10L)).thenReturn(context);
    when(sessions.findOwned(1L, 10L))
        .thenAnswer(invocation -> LearningSessionSnapshot.from(session));
    when(sessions.findOwnedCompletedForUpdate(1L, 10L))
        .thenAnswer(invocation -> LearningSessionSnapshot.from(session));
    when(sessions.isLatestCompletedScenario(any(LearningSessionSnapshot.class))).thenReturn(true);
    var queued = new java.util.concurrent.atomic.AtomicReference<Runnable>();
    var service = assessmentService(clock, queued::set, "2026-06-01T00:00:00Z");
    service.startIfNeeded(1L, 10L);
    if (expiresInQueue) {
      when(clock.instant()).thenReturn(start.plusSeconds(121));
    } else {
      when(ai.generateSessionLevelAssessment(any()))
          .thenAnswer(
              invocation -> {
                when(clock.instant()).thenReturn(start.plusSeconds(121));
                return mock(
                    com.landit.landitbe.feature.learning.scenario.assessment.client.ai
                        .AiSessionLevelAssessment.class);
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

  @DisplayName("문맥 조회가 실패해도 평가 선점을 복구할 수 있으며 AI를 다시 호출하지 않는다.")
  @Test
  void contextFailureLeavesReservationRecoverableWithoutAnotherAiCall() {
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    var clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
    var session =
        LearningSession.startScenario(1L, 1L, Locale.EN, Locale.KR, LocalDateTime.now(clock));
    org.springframework.test.util.ReflectionTestUtils.setField(session, "id", 10L);
    session.completeBySystem(CompletionReason.MAX_TURNS_REACHED, LocalDateTime.now(clock));
    session.prepareLevelAssessment(LocalDateTime.now(clock).minusMinutes(3));
    when(sessions.completeLevelAssessment(10L))
        .thenAnswer(
            invocation -> {
              session.completeLevelAssessment();
              return LearningSessionSnapshot.from(session);
            });
    var context = mock(LoadedSessionFeedbackContext.class);
    when(context.sessionId()).thenReturn(10L);
    when(contexts.load(1L, 10L))
        .thenThrow(new IllegalStateException("temporary failure"))
        .thenReturn(context);
    when(sessions.findOwned(1L, 10L))
        .thenAnswer(invocation -> LearningSessionSnapshot.from(session));
    when(sessions.findOwnedCompletedForUpdate(1L, 10L))
        .thenAnswer(invocation -> LearningSessionSnapshot.from(session));
    when(repository.findByLearningSessionId(10L)).thenReturn(Optional.empty());
    var service = assessmentService(clock, Runnable::run, "2026-06-01T00:00:00Z");

    service.startIfNeeded(1L, 10L);
    assertThat(session.getLevelAssessmentProcessingStatus()).isEqualTo(ProcessingStatus.PREPARING);
    service.get(1L, 10L);

    verify(evaluator).assessApplyAndSave(eq(1L), eq(context), isNull(), eq(false), any());
    assertThat(session.getLevelAssessmentProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    verifyNoInteractions(ai);
  }

  private SessionLevelAssessmentGenerationService assessmentService(
      Clock clock, org.springframework.core.task.TaskExecutor executor, String launchedAt) {
    return new SessionLevelAssessmentGenerationService(
        sessions,
        profiles,
        contexts,
        evaluator,
        repository,
        ai,
        transactions,
        executor,
        clock,
        new SessionLevelAssessmentLaunchService(
            new com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService(
                new SubscriptionProperties(launchedAt), clock),
            clock));
  }
}
