// 프리톡 메인 화면의 주제와 일일 발화 시간을 반환한다.

package com.landit.landitbe.feature.session.dto;

import java.util.List;

/** 프리톡 메인 화면의 주제와 일일 발화 시간을 반환한다. */
public record FreeTalkMainResponse(
    List<FreeTalkTopicResponse> topics,
    long dailySpeakingTimeLimitMs,
    long remainingSpeakingTimeMs) {

  private static final long DAILY_SPEAKING_TIME_LIMIT_MS = 60_000L;

  /** 활성 주제와 남은 시간을 메인 응답으로 만든다. */
  public static FreeTalkMainResponse of(
      List<FreeTalkTopicResponse> topics, long remainingSpeakingTimeMs) {
    return new FreeTalkMainResponse(topics, DAILY_SPEAKING_TIME_LIMIT_MS, remainingSpeakingTimeMs);
  }
}
