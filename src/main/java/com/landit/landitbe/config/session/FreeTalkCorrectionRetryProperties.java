// 프리톡 턴 교정을 다시 시도하는 횟수와 간격, 복구 처리량을 바인딩한다.

package com.landit.landitbe.config.session;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프리톡 턴 교정의 재시도 설정이다.
 *
 * @param maxAttempts 첫 시도를 포함한 최대 시도 횟수. 예: 3
 * @param retryDelays 실패한 뒤 다음 시도까지 기다리는 시간. n번째 시도가 실패하면 n번째 값을 쓰고, 모자라면 마지막 값을 쓴다. 예: 0초, 1분, 5분.
 *     마지막 시도가 실패하면 기다리지 않고 실패로 확정하므로, 최대 3회에서는 앞의 두 값만 쓰이고 5분은 횟수를 늘릴 때를 위한 값이다
 * @param batchSize 복구 한 번에 넘겨받는 교정 수의 상한. 예: 10
 * @param schedulingEnabled 주기 복구를 돌릴지 여부. 테스트에서는 꺼서 복구가 다른 테스트의 교정을 넘겨받지 않게 한다. 예: true
 */
@ConfigurationProperties(prefix = "landit.free-talk.correction-retry")
public record FreeTalkCorrectionRetryProperties(
    int maxAttempts, List<Duration> retryDelays, int batchSize, boolean schedulingEnabled) {

  /**
   * 재시도 설정을 검증한다.
   *
   * @throws IllegalArgumentException 시도 횟수·처리량이 양수가 아니거나 간격이 비었거나 음수일 때
   */
  public FreeTalkCorrectionRetryProperties {
    if (maxAttempts <= 0 || batchSize <= 0) {
      throw new IllegalArgumentException("프리톡 교정 재시도 횟수와 처리량은 양수여야 합니다.");
    }
    if (retryDelays == null
        || retryDelays.isEmpty()
        || retryDelays.stream().anyMatch(delay -> delay == null || delay.isNegative())) {
      throw new IllegalArgumentException("프리톡 교정 재시도 간격이 유효하지 않습니다.");
    }
    retryDelays = List.copyOf(retryDelays);
  }

  /**
   * 실패한 시도 다음에 기다릴 시간을 돌려준다.
   *
   * @param failedAttempt 방금 실패한 시도의 순번(첫 시도가 1)
   * @return 다음 시도까지의 대기 시간
   */
  public Duration delayAfter(int failedAttempt) {
    int index = Math.min(Math.max(failedAttempt, 1), retryDelays.size()) - 1;
    return retryDelays.get(index);
  }
}
