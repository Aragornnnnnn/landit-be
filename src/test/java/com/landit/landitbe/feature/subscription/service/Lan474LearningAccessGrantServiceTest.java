// 첫 무료 예약과 이미 시작한 학습의 소유자 및 24시간 완료 권한을 검증한다.

package com.landit.landitbe.feature.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.dto.UserSubscriptionSnapshot;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.service.LearningSessionService;
import com.landit.landitbe.feature.subscription.domain.FreeScenarioReservation;
import com.landit.landitbe.feature.subscription.domain.LearningAccessGrant;
import com.landit.landitbe.feature.subscription.exception.SubscriptionErrorCode;
import com.landit.landitbe.feature.subscription.exception.SubscriptionException;
import com.landit.landitbe.feature.subscription.repository.FreeScenarioReservationRepository;
import com.landit.landitbe.feature.subscription.repository.LearningAccessGrantRepository;
import com.landit.landitbe.feature.subscription.repository.SubscriptionLaunchPolicyRepository;
import com.landit.landitbe.feature.subscription.service.LearningAccessGrantService.StartAccess;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 현재 구독과 기존 학습의 완료 권한을 서로 혼동하지 않도록 검증한다. */
class Lan474LearningAccessGrantServiceTest {

  private static final long USER_ID = 10L;
  private static final long EXPRESSION_ID = 200L;
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
  private final LearningAccessGrantRepository repository =
      mock(LearningAccessGrantRepository.class);
  private final FreeScenarioReservationRepository reservations =
      mock(FreeScenarioReservationRepository.class);
  private final UserProfileService profiles = mock(UserProfileService.class);
  private final LearningProgressService progress = mock(LearningProgressService.class);
  private final LearningSessionService sessions = mock(LearningSessionService.class);
  private final Map<String, LearningAccessGrant> storedGrants = new HashMap<>();
  private final Map<Long, FreeScenarioReservation> storedReservations = new HashMap<>();
  private final SubscriptionLaunchPolicyService policies =
      new SubscriptionLaunchPolicyService(
          mock(SubscriptionLaunchPolicyRepository.class),
          new SubscriptionProperties("2026-09-11T11:00:00+09:00"),
          CLOCK);
  private final LearningAccessGrantService service =
      new LearningAccessGrantService(
          repository, reservations, policies, profiles, progress, CLOCK, sessions);

  /** Repository 대신 사용자와 대상별 저장 내용을 보관해 여러 서비스 호출의 결과를 연결한다. */
  @BeforeEach
  void setUp() {
    when(repository.findTopByUserIdAndKindAndTargetIdOrderByStartedAtDesc(
            anyLong(), anyString(), anyLong()))
        .thenAnswer(
            invocation ->
                Optional.ofNullable(
                    storedGrants.get(
                        key(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2)))));
    when(repository.save(any(LearningAccessGrant.class)))
        .thenAnswer(
            invocation -> {
              LearningAccessGrant grant = invocation.getArgument(0);
              store(grant);
              return grant;
            });
    when(reservations.existsById(anyLong()))
        .thenAnswer(invocation -> storedReservations.containsKey(invocation.getArgument(0)));
    when(reservations.findById(anyLong()))
        .thenAnswer(
            invocation -> Optional.ofNullable(storedReservations.get(invocation.getArgument(0))));
    when(reservations.save(any(FreeScenarioReservation.class)))
        .thenAnswer(
            invocation -> {
              FreeScenarioReservation reservation = invocation.getArgument(0);
              storedReservations.put(reservation.getUserId(), reservation);
              return reservation;
            });
    subscription(SubscriptionStatus.NONE, null);
  }

  /** 해지 예정 구독도 만료 직전까지 인정하고 정확한 만료 시각에는 갱신 웹훅 없이 제한한다. */
  @Test
  void premiumUsesExpirationInsteadOfOnlyWebhookStatus() {
    subscription(SubscriptionStatus.CANCELED, NOW.plusNanos(1));
    assertThat(service.premium(USER_ID)).isTrue();
    subscription(SubscriptionStatus.ACTIVE, NOW);
    assertThat(service.premium(USER_ID)).isFalse();
    subscription(SubscriptionStatus.ACTIVE, NOW.minusSeconds(1));
    assertThat(service.premium(USER_ID)).isFalse();
    subscription(SubscriptionStatus.EXPIRED, NOW.plusDays(1));
    assertThat(service.premium(USER_ID)).isFalse();
  }

  /** 첫 무료 기회는 시작한 세션에 고정되며 이후 새 대화용 기회를 재발급하지 않는다. */
  @Test
  void firstFreeReservationStaysBoundToItsOriginalSession() {
    StartAccess start = service.requireScenarioStart(USER_ID);
    assertThat(start.basis()).isEqualTo("FIRST_FREE");
    service.recordScenario(USER_ID, 100L, 300L, NOW, start);

    assertThat(service.freeReservation(USER_ID))
        .get()
        .satisfies(
            reservation -> {
              assertThat(reservation.getSessionId()).isEqualTo(100L);
              assertThat(reservation.getScenarioId()).isEqualTo(300L);
            });
    assertPremiumRequired(() -> service.requireScenarioStart(USER_ID));
    assertThat(service.allowsExisting(USER_ID, "SCENARIO", 100L, null, false)).isTrue();
    assertThat(service.allowsExisting(USER_ID, "SCENARIO", 101L, null, false)).isFalse();
    assertThat(storedReservations).hasSize(1);
  }

  /** 구 코드에서 공개 이후 대화를 완료한 계정에도 추가 무료 기회를 주지 않는다. */
  @Test
  void legacyCompletionAlreadyConsumesTheFreeOpportunity() {
    when(progress.hasClearedScenarioSince(USER_ID, policies.current().effectiveAt()))
        .thenReturn(true);

    assertPremiumRequired(() -> service.requireScenarioStart(USER_ID));
    assertThat(storedReservations).isEmpty();
  }

  /** 현재 유료 사용자의 시작은 첫 무료 기회를 소비하지 않는다. */
  @Test
  void paidStartDoesNotReserveFreeConversation() {
    subscription(SubscriptionStatus.ACTIVE, NOW.plusDays(1));
    StartAccess start = service.requireScenarioStart(USER_ID);
    service.recordScenario(USER_ID, 100L, 300L, NOW, start);

    assertThat(start.basis()).isEqualTo("PREMIUM");
    assertThat(storedReservations).isEmpty();
  }

  /** 구독이 없어져도 같은 표현의 미완료 시도만 재개하고 만료 시각을 연장하지 않는다. */
  @Test
  void resumingAnExistingExpressionPreservesItsOriginalExpiry() {
    LearningAccessGrant grant = expressionGrant(NOW.minusHours(2));
    store(grant);

    LearningAccessGrant resumed = service.startExpression(USER_ID, EXPRESSION_ID);

    assertThat(resumed.getId()).isEqualTo(grant.getId());
    assertThat(resumed.getExpiresAt()).isEqualTo(NOW.plusHours(22));
    verify(profiles).requireActiveForUpdate(USER_ID);
    verify(repository, never()).save(any());
  }

  /** 24시간 직전에는 이어갈 수 있지만 정확히 24시간이 되면 새 입력을 제한한다. */
  @Test
  void existingGrantExpiresAtExactlyTwentyFourHours() {
    store(expressionGrant(NOW.minusHours(24).plusSeconds(1)));
    assertThat(service.allowsExisting(USER_ID, "EXPRESSION", EXPRESSION_ID, null, false)).isTrue();

    store(expressionGrant(NOW.minusHours(24)));
    assertThat(service.allowsExisting(USER_ID, "EXPRESSION", EXPRESSION_ID, null, false)).isFalse();
    assertPremiumRequired(() -> service.startExpression(USER_ID, EXPRESSION_ID));
  }

  /** 완료 저장 재요청은 허용해도 완료한 권한으로 새 표현 연습을 시작할 수 없다. */
  @Test
  void completedExpressionAllowsOnlyCompletionReplay() {
    LearningAccessGrant grant = expressionGrant(NOW.minusHours(1));
    store(grant);
    service.completeExpression(USER_ID, EXPRESSION_ID);

    assertThat(service.allowsExisting(USER_ID, "EXPRESSION", EXPRESSION_ID, grant.getId(), true))
        .isTrue();
    assertThat(service.allowsExisting(USER_ID, "EXPRESSION", EXPRESSION_ID, grant.getId(), false))
        .isFalse();
    assertPremiumRequired(() -> service.startExpression(USER_ID, EXPRESSION_ID));
    assertThat(grant.getCompletedAt()).isEqualTo(NOW);
  }

  /** 다른 사용자의 권한이나 다른 시도 ID는 완료 재요청에도 사용할 수 없다. */
  @Test
  void existingGrantRejectsAnotherOwnerTargetOrAttempt() {
    LearningAccessGrant grant = expressionGrant(NOW.minusHours(1));
    store(grant);

    assertThat(
            service.allowsExisting(USER_ID + 1, "EXPRESSION", EXPRESSION_ID, grant.getId(), false))
        .isFalse();
    assertThat(
            service.allowsExisting(USER_ID, "EXPRESSION", EXPRESSION_ID + 1, grant.getId(), false))
        .isFalse();
    assertThat(service.allowsExisting(USER_ID, "EXPRESSION", EXPRESSION_ID, "other-attempt", false))
        .isFalse();
    grant.complete(NOW);
    assertThat(service.allowsExisting(USER_ID, "EXPRESSION", EXPRESSION_ID, "other-attempt", true))
        .isFalse();
  }

  /** 완료한 본인 스몰톡의 결과 복구만 허용하며 다른 세션의 신규 작업은 허용하지 않는다. */
  @Test
  void resultRetryRequiresOwnedCompletedFreeTalk() {
    var session = mock(com.landit.landitbe.feature.session.domain.LearningSession.class);
    when(sessions.findOwnedIfPresent(USER_ID, 100L)).thenReturn(Optional.of(session));
    when(session.getSessionType())
        .thenReturn(com.landit.landitbe.feature.session.domain.SessionType.FREE_TALK);
    when(session.getStatus())
        .thenReturn(com.landit.landitbe.feature.session.domain.LearningSessionStatus.IN_PROGRESS);
    assertThat(service.ownsCompletedFreeTalk(USER_ID, 100L)).isFalse();
    when(session.getStatus())
        .thenReturn(com.landit.landitbe.feature.session.domain.LearningSessionStatus.COMPLETED);
    assertThat(service.ownsCompletedFreeTalk(USER_ID, 100L)).isTrue();
    assertThat(service.ownsCompletedFreeTalk(USER_ID + 1, 100L)).isFalse();
  }

  /** 필터 밖 호출에서도 만료되거나 없는 표현 학습의 완료를 거부한다. */
  @Test
  void completionServiceRejectsMissingOrExpiredExpressionGrants() {
    assertPremiumRequired(() -> service.completeExpression(USER_ID, EXPRESSION_ID));
    store(expressionGrant(NOW.minusHours(24)));
    assertPremiumRequired(() -> service.completeExpression(USER_ID, EXPRESSION_ID));
  }

  /** 유료 사용자의 늦은 완료 요청도 새 학습 시도를 닫을 수 없다. */
  @Test
  void staleCompletionCannotCloseTheLatestPaidAttempt() {
    subscription(SubscriptionStatus.ACTIVE, NOW.plusDays(1));
    LearningAccessGrant current = expressionGrant(NOW);
    store(current);
    assertThatThrownBy(() -> service.completeExpression(USER_ID, EXPRESSION_ID, "older-attempt"))
        .isInstanceOf(com.landit.landitbe.shared.exception.ApiException.class);
    assertThat(current.getCompletedAt()).isNull();
    service.completeExpression(USER_ID, EXPRESSION_ID, current.getId());
    assertThat(current.getCompletedAt()).isEqualTo(NOW);
  }

  private void subscription(SubscriptionStatus status, LocalDateTime expiresAt) {
    when(profiles.getSubscription(USER_ID))
        .thenReturn(
            new UserSubscriptionSnapshot(status, status.isPremium(), null, expiresAt, null, null));
  }

  private LearningAccessGrant expressionGrant(LocalDateTime startedAt) {
    return new LearningAccessGrant(
        USER_ID, "EXPRESSION", EXPRESSION_ID, 1, "BEFORE_LAUNCH", startedAt);
  }

  private void store(LearningAccessGrant grant) {
    storedGrants.put(key(grant.getUserId(), grant.getKind(), grant.getTargetId()), grant);
  }

  private String key(long userId, String kind, long targetId) {
    return userId + ":" + kind + ":" + targetId;
  }

  private void assertPremiumRequired(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOfSatisfying(
            SubscriptionException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(SubscriptionErrorCode.PREMIUM_REQUIRED));
  }
}
