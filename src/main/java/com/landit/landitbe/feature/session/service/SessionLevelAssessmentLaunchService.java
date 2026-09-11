// 결제 도입 설정으로 수준 평가의 활성 여부와 대상 세션 경계를 결정한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 결제 도입 전 평가 호출과 도입 전 세션의 소급 처리를 차단한다. */
@Component
class SessionLevelAssessmentLaunchService {

  private final @Nullable LocalDateTime launchedAt;

  SessionLevelAssessmentLaunchService(SubscriptionProperties properties, Clock clock) {
    this.launchedAt =
        properties
            .launchedAtOrEmpty()
            .map(value -> value.atZoneSameInstant(clock.getZone()).toLocalDateTime())
            .orElse(null);
  }

  boolean isEnabled() {
    return launchedAt != null;
  }

  boolean includes(@Nullable LocalDateTime completedAt) {
    return launchedAt != null && completedAt != null && !completedAt.isBefore(launchedAt);
  }

  LocalDateTime requireLaunchedAt() {
    if (launchedAt == null) {
      throw new IllegalStateException("수준 평가가 아직 활성화되지 않았습니다.");
    }
    return launchedAt;
  }
}
