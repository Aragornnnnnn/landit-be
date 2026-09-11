// 프리톡 한도 설정이 0 이하일 때 시작 단계에서 거절되는지 검증한다.

package com.landit.landitbe.config.session;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FreeTalkPropertiesTest {

  @ParameterizedTest
  @CsvSource({"0,1000,20", "-1,1000,20", "7200000,0,20", "7200000,1000,-1"})
  void rejectsNonpositiveLimits(long speakingTime, int dailyRequests, int minuteRequests) {
    assertThatThrownBy(() -> new FreeTalkProperties(speakingTime, dailyRequests, minuteRequests))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
