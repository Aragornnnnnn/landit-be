// 프리톡 메인 화면의 주제와 일일 발화 시간을 반환한다.

package com.landit.landitbe.feature.session.dto;

import java.util.List;

/**
 * 프리톡 메인 화면의 주제와 일일 발화 시간을 반환한다.
 *
 * @param topics 활성 추천 주제
 * @param dailySpeakingTimeLimitMs 일일 사용자 발화 시간 제한 밀리초
 * @param usedSpeakingTimeMs KST 당일 사용한 사용자 발화 시간 밀리초
 * @param remainingSpeakingTimeMs KST 당일 남은 사용자 발화 시간 밀리초
 * @param canStart 새 프리톡 세션을 시작할 수 있는지 여부
 */
public record FreeTalkMainResponse(
    List<FreeTalkTopicResponse> topics,
    long dailySpeakingTimeLimitMs,
    long usedSpeakingTimeMs,
    long remainingSpeakingTimeMs,
    boolean canStart) {

  private static final long DAILY_SPEAKING_TIME_LIMIT_MS = 60_000L;

  /**
   * 활성 주제와 일일 발화 시간을 메인 응답으로 만든다.
   *
   * @param topics 활성 추천 주제
   * @param usedSpeakingTimeMs KST 당일 사용한 사용자 발화 시간 밀리초
   * @param remainingSpeakingTimeMs KST 당일 남은 사용자 발화 시간 밀리초
   * @return 프리톡 메인 응답
   */
  public static FreeTalkMainResponse of(
      List<FreeTalkTopicResponse> topics, long usedSpeakingTimeMs, long remainingSpeakingTimeMs) {
    return new FreeTalkMainResponse(
        topics,
        DAILY_SPEAKING_TIME_LIMIT_MS,
        usedSpeakingTimeMs,
        remainingSpeakingTimeMs,
        remainingSpeakingTimeMs > 0);
  }
}
