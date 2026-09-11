// 결제 도입 시각의 시간대 변환과 평가 대상의 포함 경계를 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(strings = {"2026-07-01T00:00:00Z", "2026-07-01T09:00:00+09:00"})
  void usesServiceTimeZoneAndIncludesExactLaunchInstant(String launchedAt) {
    var launch =
        new SessionLevelAssessmentLaunchService(
            new com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService(
                new SubscriptionProperties(launchedAt), clock),
            clock);
    var boundary = LocalDateTime.parse("2026-07-01T09:00:00");
    assertThat(launch.isEnabled()).isTrue();
    assertThat(launch.requireLaunchedAt()).isEqualTo(boundary);
    assertThat(launch.includes(1L, null)).isFalse();
    assertThat(launch.includes(1L, boundary.minusNanos(1))).isFalse();
    assertThat(launch.includes(1L, boundary)).isTrue();
    assertThat(launch.includes(1L, boundary.plusNanos(1))).isTrue();
  }

  @Test
  void enablesWithoutRestartOnlyWhenCurrentTimeReachesLaunch() {
    var movingClock = mock(Clock.class);
    Instant launchInstant = clock.instant();
    when(movingClock.getZone()).thenReturn(clock.getZone());
    when(movingClock.instant()).thenReturn(launchInstant.minusNanos(1));
    var launch =
        new SessionLevelAssessmentLaunchService(
            new com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService(
                new SubscriptionProperties("2026-07-01T09:00:00+09:00"), movingClock),
            movingClock);
    LocalDateTime boundary = LocalDateTime.now(clock);

    assertThat(launch.isEnabled()).isFalse();
    assertThat(launch.includes(1L, boundary)).isFalse();
    assertThat(launch.includes(1L, boundary.plusDays(1))).isFalse();

    when(movingClock.instant()).thenReturn(launchInstant);
    assertThat(launch.isEnabled()).isTrue();
    assertThat(launch.includes(1L, boundary)).isTrue();
    assertThat(launch.includes(1L, boundary.minusNanos(1))).isFalse();

    when(movingClock.instant()).thenReturn(launchInstant.plusSeconds(1));
    assertThat(launch.isEnabled()).isTrue();
    assertThat(launch.includes(1L, boundary)).isTrue();
  }
}
