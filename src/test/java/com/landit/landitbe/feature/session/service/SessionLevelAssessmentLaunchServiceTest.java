// 결제 도입 시각의 시간대 변환과 평가 대상의 포함 경계를 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SessionLevelAssessmentLaunchServiceTest {
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void blankSettingDisablesAssessmentRegardlessOfCompletionDate() {
    var launch =
        new SessionLevelAssessmentLaunchService(
            new com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService(
                new SubscriptionProperties("  "), clock),
            clock);
    assertThat(launch.isEnabled()).isFalse();
    assertThat(launch.includes(1L, LocalDateTime.now(clock))).isFalse();
    assertThatThrownBy(launch::requireLaunchedAt).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void usesServiceTimeZoneAndIncludesExactLaunchInstant() {
    var launch =
        new SessionLevelAssessmentLaunchService(
            new com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService(
                new SubscriptionProperties("2026-07-01T00:00:00Z"), clock),
            clock);
    var boundary = LocalDateTime.parse("2026-07-01T09:00:00");
    assertThat(launch.isEnabled()).isTrue();
    assertThat(launch.requireLaunchedAt()).isEqualTo(boundary);
    assertThat(launch.includes(1L, null)).isFalse();
    assertThat(launch.includes(1L, boundary.minusNanos(1))).isFalse();
    assertThat(launch.includes(1L, boundary)).isTrue();
    assertThat(launch.includes(1L, boundary.plusNanos(1))).isTrue();
  }
}
