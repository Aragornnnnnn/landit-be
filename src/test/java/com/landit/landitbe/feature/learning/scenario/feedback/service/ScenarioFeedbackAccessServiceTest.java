// 구독 정책과 세션 이력의 경계를 유지하면서 첫 완료 상세 피드백 공개 규칙을 검증한다.

package com.landit.landitbe.feature.learning.scenario.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.domain.SessionType;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionAccess;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.scenario.session.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.feature.learning.scenario.session.service.ScenarioSessionService;
import com.landit.landitbe.feature.subscription.dto.FreeScenarioAccess;
import com.landit.landitbe.feature.subscription.service.LearningAccessGrantService;
import com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService;
import com.landit.landitbe.shared.exception.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 공개 구독 계약과 소유 세션 이력으로 상세 피드백의 공개 범위를 검증한다. */
class ScenarioFeedbackAccessServiceTest {
  private static final long USER_ID = 10L;
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
  private final LearningAccessGrantService grants = mock(LearningAccessGrantService.class);
  private final LearningSessionService sessions = mock(LearningSessionService.class);
  private final ScenarioSessionService scenarioSessions = mock(ScenarioSessionService.class);
  private final SubscriptionLaunchPolicyService policies =
      new SubscriptionLaunchPolicyService(
          new SubscriptionProperties("2026-09-11T11:00:00+09:00"), CLOCK);
  private final ScenarioFeedbackAccessService service =
      new ScenarioFeedbackAccessService(policies, grants, sessions, scenarioSessions);

  @DisplayName("구독 정책 도입 전에는 상세 피드백을 잠그지 않는다.")
  @Test
  void detailFeedbackStaysOpenBeforeLaunch() {
    var beforeLaunch =
        new ScenarioFeedbackAccessService(
            new SubscriptionLaunchPolicyService(
                new SubscriptionProperties("2026-09-12T11:00:00+09:00"), CLOCK),
            grants,
            sessions,
            scenarioSessions);
    assertThat(beforeLaunch.detailFeedbackLocked(USER_ID, 100L)).isFalse();
    verify(sessions, never()).findOwnedIfPresent(anyLong(), anyLong());
  }

  @DisplayName("프리미엄 사용자는 상세 피드백을 잠그지 않는다.")
  @Test
  void detailFeedbackStaysOpenForPremium() {
    when(grants.premium(USER_ID)).thenReturn(true);
    assertThat(service.detailFeedbackLocked(USER_ID, 100L)).isFalse();
    verify(sessions, never()).findOwnedIfPresent(anyLong(), anyLong());
  }

  @DisplayName("도입 전에 시작한 세션은 도입 후에 완료해도 상세 피드백을 잠그지 않는다.")
  @Test
  void detailFeedbackStaysOpenForPreLaunchSession() {
    when(grants.freeReservation(USER_ID))
        .thenReturn(Optional.of(new FreeScenarioAccess(101L, 300L)));
    ownedSession(100L, policies.current().effectiveAt().minusNanos(1));
    assertThat(service.detailFeedbackLocked(USER_ID, 100L)).isFalse();
    verify(scenarioSessions, never()).requireMessageContext(anyLong());
  }

  @DisplayName("무료 시나리오 예약이 없으면 소유 세션의 상세 피드백을 잠그지 않는다.")
  @Test
  void detailFeedbackStaysOpenWithoutReservation() {
    when(grants.freeReservation(USER_ID)).thenReturn(Optional.empty());
    ownedSession(101L, NOW);
    assertThat(service.detailFeedbackLocked(USER_ID, 101L)).isFalse();
    verify(scenarioSessions, never()).requireMessageContext(anyLong());
  }

  @DisplayName("첫 시나리오를 처음 완료한 세션은 상세 피드백을 공개한다.")
  @Test
  void detailFeedbackOpensOnlyForFirstCompletionOfFirstScenario() {
    when(grants.freeReservation(USER_ID))
        .thenReturn(Optional.of(new FreeScenarioAccess(100L, 300L)));
    ownedSession(100L, NOW);
    scenarioOf(100L, 300L);
    LocalDateTime effectiveAt = policies.current().effectiveAt();
    when(scenarioSessions.isFirstCompletedSince(USER_ID, 300L, effectiveAt, 100L)).thenReturn(true);

    assertThat(service.detailFeedbackLocked(USER_ID, 100L)).isFalse();
  }

  @DisplayName("첫 시나리오라도 재완료한 세션은 상세 피드백을 잠근다.")
  @Test
  void detailFeedbackLocksRepeatedCompletionOfFirstScenario() {
    when(grants.freeReservation(USER_ID))
        .thenReturn(Optional.of(new FreeScenarioAccess(100L, 300L)));
    ownedSession(101L, NOW.plusDays(1));
    scenarioOf(101L, 300L);
    LocalDateTime effectiveAt = policies.current().effectiveAt();
    when(scenarioSessions.isFirstCompletedSince(USER_ID, 300L, effectiveAt, 101L))
        .thenReturn(false);

    assertThat(service.detailFeedbackLocked(USER_ID, 101L)).isTrue();
  }

  @DisplayName("예약한 첫 시나리오와 다른 시나리오는 상세 피드백을 잠근다.")
  @Test
  void detailFeedbackLocksOtherScenarioWithoutCheckingFirstCompletion() {
    when(grants.freeReservation(USER_ID))
        .thenReturn(Optional.of(new FreeScenarioAccess(100L, 300L)));
    ownedSession(102L, NOW.plusDays(2));
    scenarioOf(102L, 301L);

    assertThat(service.detailFeedbackLocked(USER_ID, 102L)).isTrue();
    verify(scenarioSessions, never())
        .isFirstCompletedSince(USER_ID, 301L, policies.current().effectiveAt(), 102L);
  }

  /** 소유 세션이 없으면 예약 부재만으로 상세 피드백을 공개하지 않는다. */
  @DisplayName("소유 세션이 없으면 예약 부재만으로 상세 피드백을 공개하지 않는다.")
  @Test
  void missingOwnedSessionFailsBeforeReservationLookup() {
    assertThatThrownBy(() -> service.detailFeedbackLocked(USER_ID, 100L))
        .isInstanceOf(ApiException.class);
    verify(grants, never()).freeReservation(anyLong());
  }

  private void ownedSession(long sessionId, LocalDateTime startedAt) {
    when(sessions.findOwnedIfPresent(USER_ID, sessionId))
        .thenReturn(
            Optional.of(
                new LearningSessionAccess(
                    SessionType.SCENARIO, LearningSessionStatus.COMPLETED, startedAt)));
  }

  private void scenarioOf(long sessionId, long scenarioId) {
    var context = mock(ScenarioSessionMessageContextProjection.class);
    when(context.scenarioId()).thenReturn(scenarioId);
    when(scenarioSessions.requireMessageContext(sessionId)).thenReturn(context);
  }
}
