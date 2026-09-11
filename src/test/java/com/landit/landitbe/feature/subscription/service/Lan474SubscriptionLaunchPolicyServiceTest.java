// SSM 설정만으로 선배포와 결제 오픈 및 롤백이 결정되는지 검증한다.

package com.landit.landitbe.feature.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** DB 공개 정책 없이 기존 환경변수 전환 계약을 유지한다. */
class Lan474SubscriptionLaunchPolicyServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));

  /** 미설정 선배포와 오픈 시각을 비운 롤백은 모든 계정의 잠금을 해제한다. */
  @Test
  void blankLaunchSettingDisablesRestrictionsBeforeReleaseAndOnRollback() {
    var enabled = service("2026-09-11T02:00:00Z");
    assertThat(enabled.enabledFor(enabled.current(), 10L)).isTrue();
    for (String setting : new String[] {"", "  "}) {
      var disabled = service(setting);
      assertThat(disabled.active(disabled.current())).isFalse();
      assertThat(disabled.enabledFor(disabled.current(), 10L)).isFalse();
      assertThat(disabled.enabledFor(disabled.current(), 20L)).isFalse();
      assertThat(disabled.current().newStartsPaused()).isFalse();
    }
  }

  /** 시각의 시간대를 변환하고 정확한 경계부터 활성화한다. */
  @Test
  void launchUsesTheSameInstantAndWaitsForItsBoundary() {
    var future = service("2026-09-11T03:00:01Z");
    assertThat(future.current().effectiveAt()).isEqualTo(LocalDateTime.now(CLOCK).plusSeconds(1));
    assertThat(future.active(future.current())).isFalse();
    var due = service("2026-09-11T12:00:00+09:00");
    assertThat(due.active(due.current())).isTrue();
    assertThat(due.enabledFor(due.current(), 10L)).isTrue();
    assertThat(due.enabledFor(due.current(), 20L)).isTrue();
  }

  private SubscriptionLaunchPolicyService service(String setting) {
    return new SubscriptionLaunchPolicyService(new SubscriptionProperties(setting), CLOCK);
  }
}
