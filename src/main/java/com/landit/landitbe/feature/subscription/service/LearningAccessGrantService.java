// 새 학습 권한과 이미 시작한 학습의 24시간 완료 권한을 구분하고, 무료 사용자의 상세 피드백 잠금을 판단한다.

package com.landit.landitbe.feature.subscription.service;

import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.subscription.domain.FreeScenarioReservation;
import com.landit.landitbe.feature.subscription.domain.LearningAccessGrant;
import com.landit.landitbe.feature.subscription.dto.ExpressionLearningAttempt;
import com.landit.landitbe.feature.subscription.dto.FreeScenarioAccess;
import com.landit.landitbe.feature.subscription.dto.StartAccess;
import com.landit.landitbe.feature.subscription.exception.SubscriptionErrorCode;
import com.landit.landitbe.feature.subscription.exception.SubscriptionException;
import com.landit.landitbe.feature.subscription.repository.FreeScenarioReservationRepository;
import com.landit.landitbe.feature.subscription.repository.LearningAccessGrantRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 잠금 아래 첫 시나리오 예약과 학습별 권한을 발급하고, 무료 사용자의 상세 피드백 잠금을 판단한다. */
@Service
@RequiredArgsConstructor
public class LearningAccessGrantService {
  private final LearningAccessGrantRepository grants;
  private final FreeScenarioReservationRepository reservations;
  private final SubscriptionLaunchPolicyService policies;
  private final UserProfileService profiles;
  private final Clock clock;
  private final com.landit.landitbe.feature.session.service.LearningSessionService sessions;
  private final com.landit.landitbe.feature.session.service.ScenarioSessionService scenarioSessions;

  /**
   * 현재 사용자가 결제 제한 대상인지 확인한다.
   *
   * @param userId 사용자 ID
   * @return 현재 시각에 정책이 적용되면 true
   */
  public boolean paymentEnabled(long userId) {
    return policies.enabledFor(policies.current(), userId);
  }

  /**
   * 현재 구독의 실제 만료 시각까지 프리미엄을 인정한다.
   *
   * @param userId 학습 사용자 ID
   * @return 실제 만료 전 프리미엄이면 true
   */
  public boolean premium(long userId) {
    var snapshot = profiles.getSubscription(userId);
    return snapshot.premium()
        && (snapshot.expiresAt() == null
            || LocalDateTime.now(clock).isBefore(snapshot.expiresAt()));
  }

  /**
   * 도입 후 무료 상태로 처음 시작한 시나리오의 예약을 반환한다. 예약은 한 번만 기록하며 다른 시나리오로 옮기지 않는다.
   *
   * @param userId 학습 사용자 ID
   * @return 첫 시나리오 예약 또는 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<FreeScenarioAccess> freeReservation(long userId) {
    return reservations
        .findById(userId)
        .map(
            reservation ->
                new FreeScenarioAccess(reservation.getSessionId(), reservation.getScenarioId()));
  }

  /**
   * 새 시나리오 시작 전에 호출한다. 호출자는 사용자 잠금을 유지해야 한다.
   *
   * <p>시나리오 대화는 구독과 관계없이 허용한다. 무료 사용자도 세션 시작·메시지 전송·완료·총 피드백(점수·요약) 조회까지 할 수 있고, 잠기는 것은 메시지별 상세
   * 피드백뿐이다. 무료 사용자의 첫 시작은 FIRST_FREE로 판정해 첫 시나리오 예약을 남기고, 이후 시작은 FREE로 판정한다(둘 다 허용 범위는 같고 예약 여부만
   * 다르다). 첫 시나리오 예약은 상세 피드백 잠금({@link #detailFeedbackLocked})의 기준이 된다.
   *
   * @param userId 학습 사용자 ID
   * @return 허용된 시작의 정책 버전과 근거
   * @throws ApiException 배포 전환으로 새 학습 시작이 중지됐을 때
   */
  public StartAccess requireScenarioStart(long userId) {
    var policy = policies.current();
    requireStartsOpen(policy);
    boolean enabled = policies.enabledFor(policy, userId);
    if (!enabled || premium(userId)) {
      return new StartAccess(policy.version(), enabled ? "PREMIUM" : "BEFORE_LAUNCH");
    }
    return new StartAccess(
        policy.version(), reservations.existsById(userId) ? "FREE" : "FIRST_FREE");
  }

  /**
   * 시작한 시나리오의 권한과, 첫 시작이면 첫 시나리오 예약을 같은 트랜잭션에 저장한다.
   *
   * @param userId 학습 사용자 ID
   * @param sessionId 학습 세션 ID
   * @param scenarioId 시나리오 ID
   * @param startedAt 원래 학습 시작 시각
   * @param access 시작할 때 판정한 권한 근거
   */
  @Transactional
  public void recordScenario(
      long userId, long sessionId, long scenarioId, LocalDateTime startedAt, StartAccess access) {
    boolean free = "FIRST_FREE".equals(access.basis());
    grants.save(
        new LearningAccessGrant(
            userId, "SCENARIO", sessionId, access.policyVersion(), access.basis(), startedAt));
    if (free) {
      reservations.save(new FreeScenarioReservation(userId, sessionId, scenarioId, startedAt));
    }
  }

  /**
   * 유료 학습의 새 시작을 검사한다.
   *
   * @param userId 학습 사용자 ID
   * @return 허용된 시작의 정책 버전과 근거
   */
  public StartAccess requirePremiumStart(long userId) {
    var policy = policies.current();
    requireStartsOpen(policy);
    if (policies.enabledFor(policy, userId) && !premium(userId)) {
      throw new SubscriptionException(SubscriptionErrorCode.PREMIUM_REQUIRED);
    }
    return new StartAccess(
        policy.version(), policies.enabledFor(policy, userId) ? "PREMIUM" : "BEFORE_LAUNCH");
  }

  /**
   * 허용된 스몰톡 시작에 완료 권한을 저장한다.
   *
   * @param userId 학습 사용자 ID
   * @param sessionId 학습 세션 ID
   * @param startedAt 원래 학습 시작 시각
   * @param access 시작할 때 판정한 권한 근거
   */
  @Transactional
  public void recordFreeTalk(
      long userId, long sessionId, LocalDateTime startedAt, StartAccess access) {
    grants.save(
        new LearningAccessGrant(
            userId, "FREE_TALK", sessionId, access.policyVersion(), access.basis(), startedAt));
  }

  /**
   * 표현을 시작하거나 같은 미완료 시도를 재개한다.
   *
   * @param userId 학습 사용자 ID
   * @param expressionId 표현 ID
   * @return 새로 발급하거나 재개한 원래 학습 권한
   */
  @Transactional
  public ExpressionLearningAttempt startExpression(long userId, long expressionId) {
    profiles.requireActiveForUpdate(userId);
    var existing = latest(userId, "EXPRESSION", expressionId);
    if (existing.isPresent() && valid(existing.get(), false)) {
      return ExpressionLearningAttempt.from(existing.get());
    }
    StartAccess access = requirePremiumStart(userId);
    return ExpressionLearningAttempt.from(
        grants.save(
            new LearningAccessGrant(
                userId,
                "EXPRESSION",
                expressionId,
                access.policyVersion(),
                access.basis(),
                LocalDateTime.now(clock))));
  }

  /**
   * 필터에서 요청 대상과 소유자에 묶인 기존 권한만 허용한다.
   *
   * @param userId 학습 사용자 ID
   * @param kind SCENARIO, FREE_TALK 또는 EXPRESSION
   * @param targetId 학습 대상 ID
   * @param attemptId 학습 시작 시도 ID 또는 구버전의 null
   * @param completion 완료 저장 재요청 여부
   * @return 같은 소유 학습의 요청을 허용하면 true
   */
  @Transactional(readOnly = true)
  public boolean allowsExisting(
      long userId, String kind, long targetId, String attemptId, boolean completion) {
    var stored = latest(userId, kind, targetId);
    if (stored.isPresent()) {
      return (attemptId == null || stored.get().getId().equals(attemptId))
          && valid(stored.get(), completion);
    }
    // 롤링 교체 중 구 BE가 만든 세션도 오픈 전 시작 기록으로 판별한다.
    if (!"EXPRESSION".equals(kind)) {
      var policy = policies.current();
      if (policy.effectiveAt() == null) {
        return false;
      }
      return sessions
          .findOwnedIfPresent(userId, targetId)
          .filter(session -> session.sessionType().name().equals(kind))
          .filter(session -> session.startedAt().isBefore(policy.effectiveAt()))
          .filter(session -> LocalDateTime.now(clock).isBefore(session.startedAt().plusHours(24)))
          .isPresent();
    }
    return false;
  }

  /**
   * 이미 완료한 스몰톡 결과의 재생성만 허용할 소유권을 확인한다.
   *
   * @param userId 소유자 ID
   * @param sessionId 완료된 스몰톡 ID
   * @return 본인의 완료된 스몰톡이면 true
   */
  @Transactional(readOnly = true)
  public boolean ownsCompletedFreeTalk(long userId, long sessionId) {
    return sessions
        .findOwnedIfPresent(userId, sessionId)
        .filter(session -> session.sessionType().name().equals("FREE_TALK"))
        .filter(
            session ->
                session.status()
                    == com.landit.landitbe.feature.session.domain.LearningSessionStatus.COMPLETED)
        .isPresent();
  }

  /**
   * 프리톡의 새 발화 입력에만 적용하고 이미 접수된 발화의 결과 저장에는 적용하지 않는다. 시나리오 대화는 제한하지 않으므로 호출하지 않는다.
   *
   * @param userId 학습 사용자 ID
   * @param kind FREE_TALK 또는 EXPRESSION
   * @param sessionId 학습 세션 ID
   * @throws SubscriptionException 미결제 상태에서 이어갈 권한이 없거나 만료됐을 때
   */
  public void requireSessionContinuation(long userId, String kind, long sessionId) {
    if (!policies.enabledFor(policies.current(), userId)
        || premium(userId)
        || allowsExisting(userId, kind, sessionId, null, false)) {
      return;
    }
    throw new SubscriptionException(SubscriptionErrorCode.PREMIUM_REQUIRED);
  }

  /**
   * 무료 사용자가 볼 수 없는 상세 피드백(메시지별 피드백)인지 판단한다.
   *
   * <p>도입 전이거나 프리미엄이면 잠그지 않는다. 도입 전에 시작한 세션은 도입 후에 끝났어도 잠그지 않고 첫 시나리오 기회도 소모하지 않는다. 그 외에는 첫 시나리오
   * 예약과 비교해, 예약된 시나리오의 도입 후 세션 가운데 처음 완료한 세션만 허용하고 나머지(같은 시나리오의 재완료, 다른 시나리오)는 잠근다. 예약이 없으면 도입 후
   * 프리미엄으로만 시작한 사용자이므로 학습 보존 취지대로 잠그지 않는다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 완료된 시나리오 학습 세션 ID
   * @return 메시지별 피드백을 비워 내려야 하면 true
   * @throws ApiException 소유한 세션을 찾지 못했을 때
   */
  @Transactional(readOnly = true)
  public boolean detailFeedbackLocked(long userId, long sessionId) {
    SubscriptionLaunchPolicyService.Policy policy = policies.current();
    if (!policies.enabledFor(policy, userId) || premium(userId)) {
      return false;
    }
    LearningSession session =
        sessions
            .findOwnedIfPresent(userId, sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    if (session.getStartedAt().isBefore(policy.effectiveAt())) {
      return false;
    }
    Optional<FreeScenarioReservation> reservation = reservations.findById(userId);
    if (reservation.isEmpty()) {
      return false;
    }
    long scenarioId = scenarioSessions.requireMessageContext(sessionId).scenarioId();
    if (scenarioId != reservation.get().getScenarioId()) {
      return true;
    }
    return !scenarioSessions.isFirstCompletedSince(
        userId, scenarioId, policy.effectiveAt(), sessionId);
  }

  /**
   * 표현 완료 저장과 같은 트랜잭션에서 시도를 확정한다.
   *
   * @param userId 학습 사용자 ID
   * @param expressionId 표현 ID
   */
  @Transactional
  public void completeExpression(long userId, long expressionId) {
    completeExpression(userId, expressionId, null);
  }

  /**
   * 같은 시도의 완료만 확정해 이전 기기의 응답이 새 시도를 닫지 못하게 한다.
   *
   * @param userId 학습 사용자 ID
   * @param expressionId 표현 ID
   * @param attemptId 시작 시도 ID 또는 구버전의 null
   * @throws ApiException 다른 시도의 ID를 전달했을 때
   * @throws SubscriptionException 미결제 상태에서 완료 권한이 없거나 만료됐을 때
   */
  @Transactional
  public void completeExpression(long userId, long expressionId, String attemptId) {
    profiles.requireActiveForUpdate(userId);
    var current = latest(userId, "EXPRESSION", expressionId);
    if (attemptId != null && (current.isEmpty() || !current.get().getId().equals(attemptId))) {
      throw new ApiException(ErrorCode.CONFLICT, "현재 학습 시도와 다릅니다.");
    }
    if (policies.enabledFor(policies.current(), userId)
        && !premium(userId)
        && !allowsExisting(userId, "EXPRESSION", expressionId, null, true)) {
      throw new SubscriptionException(SubscriptionErrorCode.PREMIUM_REQUIRED);
    }
    latest(userId, "EXPRESSION", expressionId)
        .ifPresent(grant -> grant.complete(LocalDateTime.now(clock)));
  }

  private Optional<LearningAccessGrant> latest(long userId, String kind, long targetId) {
    return grants.findTopByUserIdAndKindAndTargetIdOrderByStartedAtDesc(userId, kind, targetId);
  }

  private boolean valid(LearningAccessGrant grant, boolean completion) {
    return (completion && grant.getCompletedAt() != null)
        || (grant.getCompletedAt() == null
            && LocalDateTime.now(clock).isBefore(grant.getExpiresAt()));
  }

  private void requireStartsOpen(SubscriptionLaunchPolicyService.Policy policy) {
    if (policy.newStartsPaused()) {
      throw new ApiException(ErrorCode.CONFLICT, "배포 중입니다. 잠시 후 새 학습을 시작해 주세요.");
    }
  }
}
