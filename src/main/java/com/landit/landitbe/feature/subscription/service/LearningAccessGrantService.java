// 새 학습 권한과 이미 시작한 학습의 24시간 완료 권한을 구분한다.

package com.landit.landitbe.feature.subscription.service;

import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.domain.FreeScenarioReservation;
import com.landit.landitbe.feature.subscription.domain.LearningAccessGrant;
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

/** 사용자 잠금 아래 무료 기회와 학습별 권한을 발급한다. */
@Service
@RequiredArgsConstructor
public class LearningAccessGrantService {
  private final LearningAccessGrantRepository grants;
  private final FreeScenarioReservationRepository reservations;
  private final SubscriptionLaunchPolicyService policies;
  private final UserProfileService profiles;
  private final LearningProgressService progress;
  private final Clock clock;
  private final com.landit.landitbe.feature.session.service.LearningSessionService sessions;

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
   * 소모된 첫 무료 기회의 세션을 반환하며 다른 대화로 재발급하지 않는다.
   *
   * @param userId 학습 사용자 ID
   * @return 소모한 무료 기회의 예약 또는 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<FreeScenarioReservation> freeReservation(long userId) {
    return reservations.findById(userId);
  }

  /**
   * 새 시나리오 시작 전에 호출한다. 호출자는 사용자 잠금을 유지해야 한다.
   *
   * @param userId 학습 사용자 ID
   * @return 허용된 시작의 정책 버전과 근거
   */
  public StartAccess requireScenarioStart(long userId) {
    var policy = policies.current();
    requireStartsOpen(policy);
    boolean enabled = policies.enabledFor(policy, userId);
    if (!enabled || premium(userId)) {
      return new StartAccess(policy.version(), enabled ? "PREMIUM" : "BEFORE_LAUNCH");
    }
    if (reservations.existsById(userId)
        || progress.hasClearedScenarioSince(userId, policy.effectiveAt())) {
      throw new SubscriptionException(SubscriptionErrorCode.PREMIUM_REQUIRED);
    }
    return new StartAccess(policy.version(), "FIRST_FREE");
  }

  /**
   * 시작한 시나리오와 첫 무료 예약을 같은 트랜잭션에 저장한다.
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
  public LearningAccessGrant startExpression(long userId, long expressionId) {
    profiles.requireActiveForUpdate(userId);
    var existing = latest(userId, "EXPRESSION", expressionId);
    if (existing.isPresent() && valid(existing.get(), false)) {
      return existing.get();
    }
    StartAccess access = requirePremiumStart(userId);
    return grants.save(
        new LearningAccessGrant(
            userId,
            "EXPRESSION",
            expressionId,
            access.policyVersion(),
            access.basis(),
            LocalDateTime.now(clock)));
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
          .filter(session -> session.getSessionType().name().equals(kind))
          .filter(session -> session.getStartedAt().isBefore(policy.effectiveAt()))
          .filter(
              session -> LocalDateTime.now(clock).isBefore(session.getStartedAt().plusHours(24)))
          .isPresent();
    }
    return false;
  }

  /**
   * 저장 응답의 재전송 후보가 요청자의 시나리오인지 확인한다.
   *
   * @param userId 학습 사용자 ID
   * @param sessionId 학습 세션 ID
   * @return 본인의 시나리오 세션이면 true
   */
  @Transactional(readOnly = true)
  public boolean ownsScenario(long userId, long sessionId) {
    return sessions
        .findOwnedIfPresent(userId, sessionId)
        .filter(session -> session.getSessionType().name().equals("SCENARIO"))
        .isPresent();
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
        .filter(session -> session.getSessionType().name().equals("FREE_TALK"))
        .filter(
            session ->
                session.getStatus()
                    == com.landit.landitbe.feature.session.domain.LearningSessionStatus.COMPLETED)
        .isPresent();
  }

  /**
   * 새 발화 입력에만 적용하고 이미 접수된 발화의 결과 저장에는 적용하지 않는다.
   *
   * @param userId 학습 사용자 ID
   * @param kind SCENARIO, FREE_TALK 또는 EXPRESSION
   * @param sessionId 학습 세션 ID
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

  /**
   * 시작 시점에 한 번 결정해 저장까지 유지하는 권한 근거다.
   *
   * @param policyVersion 시작 시 적용한 공개 정책 버전
   * @param basis 학습 허용 근거
   */
  public record StartAccess(long policyVersion, String basis) {}

  private void requireStartsOpen(SubscriptionLaunchPolicyService.Policy policy) {
    if (policy.newStartsPaused()) {
      throw new ApiException(ErrorCode.CONFLICT, "배포 중입니다. 잠시 후 새 학습을 시작해 주세요.");
    }
  }
}
