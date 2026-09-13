// 구독 정책과 세션 완료 이력을 조합해 시나리오 상세 피드백의 공개 범위를 판단한다.

package com.landit.landitbe.feature.session.feedback.service;

import com.landit.landitbe.feature.session.service.LearningSessionService;
import com.landit.landitbe.feature.session.scenario.service.ScenarioSessionService;
import com.landit.landitbe.feature.subscription.service.LearningAccessGrantService;
import com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 세션이 소유한 이력과 구독의 공개 값 계약을 이용해 상세 피드백 접근을 조율한다. */
@Service
@RequiredArgsConstructor
public class ScenarioFeedbackAccessService {
  private final SubscriptionLaunchPolicyService policies;
  private final LearningAccessGrantService accessGrants;
  private final LearningSessionService sessions;
  private final ScenarioSessionService scenarioSessions;

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
    if (!policies.enabledFor(policy, userId) || accessGrants.premium(userId)) {
      return false;
    }
    var session =
        sessions
            .findOwnedIfPresent(userId, sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    if (session.startedAt().isBefore(policy.effectiveAt())) {
      return false;
    }
    var reservation = accessGrants.freeReservation(userId);
    if (reservation.isEmpty()) {
      return false;
    }
    long scenarioId = scenarioSessions.requireMessageContext(sessionId).scenarioId();
    if (scenarioId != reservation.get().scenarioId()) {
      return true;
    }
    return !scenarioSessions.isFirstCompletedSince(
        userId, scenarioId, policy.effectiveAt(), sessionId);
  }

}
