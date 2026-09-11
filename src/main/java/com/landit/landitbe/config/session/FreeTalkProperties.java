// 프리톡 발화 제한시간 설정을 바인딩한다.

package com.landit.landitbe.config.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프리톡 발화 제한시간 설정을 바인딩한다.
 *
 * @param speakingTimeLimitMs 일일 사용자 발화 제한시간 밀리초
 * @param dailyRequestLimit 계정별 일일 생성 요청 상한
 * @param requestsPerMinuteLimit 계정별 고정 1분 구간의 생성 요청 상한
 */
@ConfigurationProperties(prefix = "landit.free-talk")
public record FreeTalkProperties(
    long speakingTimeLimitMs, int dailyRequestLimit, int requestsPerMinuteLimit) {

  /**
   * 잘못된 한도 설정으로 제한이 무력화되거나 모든 요청이 차단되는 것을 방지한다.
   *
   * @throws IllegalArgumentException 한도 중 하나라도 0 이하일 때
   */
  public FreeTalkProperties {
    if (speakingTimeLimitMs <= 0 || dailyRequestLimit <= 0 || requestsPerMinuteLimit <= 0) {
      throw new IllegalArgumentException("프리톡 발화 시간과 요청 한도는 모두 0보다 커야 합니다.");
    }
  }
}
