// 결제 도입 설정으로 수준 평가의 활성 여부와 대상 세션 경계를 결정한다.

package com.landit.landitbe.feature.session.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 결제 도입 전 평가 호출과 도입 전 세션의 소급 처리를 차단한다. */
@Component
class SessionLevelAssessmentLaunchService {

  private final com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService
      policies;

  SessionLevelAssessmentLaunchService(
      com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService policies,
      Clock clock) {
    this.policies = policies;
  }

  boolean isEnabledFor(long userId) {
    return policies.enabledFor(policies.current(), userId);
  }

  boolean isEnabled() {
    return policies.active(policies.current());
  }

  boolean includes(long userId, @Nullable LocalDateTime completedAt) {
    var policy = policies.current();
    return policies.enabledFor(policy, userId)
        && completedAt != null
        && !completedAt.isBefore(policy.effectiveAt());
  }

  LocalDateTime requireLaunchedAt() {
    var policy = policies.current();
    if (!policies.active(policy)) {
      throw new IllegalStateException("수준 평가가 아직 활성화되지 않았습니다.");
    }
    return policy.effectiveAt();
  }
}
