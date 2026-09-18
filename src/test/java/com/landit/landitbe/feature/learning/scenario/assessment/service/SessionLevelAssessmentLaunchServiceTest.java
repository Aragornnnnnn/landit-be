// 결제 도입 시각의 시간대 변환과 평가 대상의 포함 경계를 검증한다.

package com.landit.landitbe.feature.learning.scenario.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SessionLevelAssessmentLaunchServiceTest {
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));

  @DisplayName("수준 평가 도입 시각이 없으면 완료 날짜와 무관하게 평가를 비활성화한다.")
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

  @DisplayName("서비스 시간대를 기준으로 정확한 도입 시각부터 수준 평가를 허용한다.")
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

  @DisplayName("현재 시각이 도입 시각에 도달하면 재시작 없이 수준 평가를 활성화한다.")
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
