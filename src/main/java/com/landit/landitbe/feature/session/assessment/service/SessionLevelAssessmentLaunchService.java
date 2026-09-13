// 결제 도입 설정으로 수준 평가의 활성 여부와 대상 세션 경계를 결정한다.

package com.landit.landitbe.feature.session.assessment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 결제 도입 전 평가 호출과 도입 전 세션의 소급 처리를 차단한다. */
@Component
public class SessionLevelAssessmentLaunchService {

  private final com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService
      policies;

  /**
   * 구독 도입 정책을 평가 활성화 판단에 연결한다.
   *
   * @param policies 구독 공개 정책
   * @param clock 기존 생성자 호환용 시계. 판단 시각은 policies가 소유한다
   */
  public SessionLevelAssessmentLaunchService(
      com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService policies,
      Clock clock) {
    this.policies = policies;
  }

  /**
   * 사용자에게 수준 평가가 공개됐는지 반환한다.
   *
   * @param userId 평가 대상 사용자
   * @return 사용자별 공개 여부
   */
  public boolean isEnabledFor(long userId) {
    return policies.enabledFor(policies.current(), userId);
  }

  /**
   * 일반 사용자 대상 공개 시각에 도달했는지 반환한다.
   *
   * @return 공개 여부
   */
  public boolean isEnabled() {
    return policies.active(policies.current());
  }

  /**
   * 공개 이후 완료된 사용자 세션인지 확인한다.
   *
   * @param userId 세션 소유자
   * @param completedAt 세션 완료 시각 또는 null
   * @return 평가 가능한 완료 세션이면 true
   */
  public boolean includes(long userId, @Nullable LocalDateTime completedAt) {
    var policy = policies.current();
    return policies.enabledFor(policy, userId)
        && completedAt != null
        && !completedAt.isBefore(policy.effectiveAt());
  }

  /**
   * 공개된 평가의 유효 시작 시각을 반환한다.
   *
   * @return 평가 도입 시각
   * @throws IllegalStateException 아직 평가가 공개되지 않았을 때
   */
  public LocalDateTime requireLaunchedAt() {
    var policy = policies.current();
    if (!policies.active(policy)) {
      throw new IllegalStateException("수준 평가가 아직 활성화되지 않았습니다.");
    }
    return policy.effectiveAt();
  }
}
