// 프리톡 턴 교정 재시도 설정의 간격 선택과 잘못된 값 거부를 검증한다.

package com.landit.landitbe.config.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 프리톡 턴 교정 재시도 설정의 간격 선택과 잘못된 값 거부를 검증한다. */
class FreeTalkCorrectionRetryPropertiesTest {

  private static final List<Duration> DELAYS =
      List.of(Duration.ZERO, Duration.ofMinutes(1), Duration.ofMinutes(5));

  @DisplayName("n번째 시도가 실패하면 n번째 간격을 쓰고, 간격이 모자라면 마지막 간격을 쓴다.")
  @Test
  void picksDelayByFailedAttempt() {
    FreeTalkCorrectionRetryProperties properties =
        new FreeTalkCorrectionRetryProperties(5, DELAYS, 10);

    assertThat(properties.delayAfter(1)).isEqualTo(Duration.ZERO);
    assertThat(properties.delayAfter(2)).isEqualTo(Duration.ofMinutes(1));
    assertThat(properties.delayAfter(3)).isEqualTo(Duration.ofMinutes(5));
    assertThat(properties.delayAfter(4)).isEqualTo(Duration.ofMinutes(5));
    // 시도 순번은 1부터지만 잘못된 값이 와도 예외 대신 첫 간격을 쓴다.
    assertThat(properties.delayAfter(0)).isEqualTo(Duration.ZERO);
  }

  @DisplayName("시도 횟수·처리량이 양수가 아니거나 간격이 비었거나 음수면 기동 시점에 거부한다.")
  @Test
  void rejectsInvalidSettings() {
    assertThatThrownBy(() -> new FreeTalkCorrectionRetryProperties(0, DELAYS, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FreeTalkCorrectionRetryProperties(3, DELAYS, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FreeTalkCorrectionRetryProperties(3, List.of(), 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FreeTalkCorrectionRetryProperties(3, null, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new FreeTalkCorrectionRetryProperties(3, List.of(Duration.ofSeconds(-1)), 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new FreeTalkCorrectionRetryProperties(3, Arrays.asList(Duration.ZERO, null), 10))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
