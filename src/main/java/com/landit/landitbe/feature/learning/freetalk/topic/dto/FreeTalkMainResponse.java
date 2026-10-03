// 프리톡 메인 화면의 주제와 일일 발화 시간을 반환한다.

package com.landit.landitbe.feature.learning.freetalk.topic.dto;

import com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkAvailableFollowUp;
import java.util.List;

/**
 * 프리톡 메인 화면의 무작위 추천 주제와 일일 발화 시간을 반환한다.
 *
 * @param topics 무작위로 뽑은 활성 추천 주제(최대 5개)
 * @param dailySpeakingTimeLimitMs 일일 사용자 발화 시간 제한 밀리초
 * @param usedSpeakingTimeMs KST 당일 사용한 사용자 발화 시간 밀리초
 * @param remainingSpeakingTimeMs KST 당일 남은 사용자 발화 시간 밀리초
 * @param canStart 새 프리톡 세션을 시작할 수 있는지 여부
 * @param followUps 캐릭터별 사용할 수 있는 지난 대화 질문
 */
public record FreeTalkMainResponse(
    List<FreeTalkTopicResponse> topics,
    long dailySpeakingTimeLimitMs,
    long usedSpeakingTimeMs,
    long remainingSpeakingTimeMs,
    boolean canStart,
    List<FreeTalkAvailableFollowUp> followUps) {

  /**
   * 응답의 목록을 방어적으로 복사한다.
   *
   * @param topics 추천 주제
   * @param dailySpeakingTimeLimitMs 일일 제한
   * @param usedSpeakingTimeMs 사용한 시간
   * @param remainingSpeakingTimeMs 남은 시간
   * @param canStart 시작 가능 여부
   * @param followUps 사용 가능한 예고 질문
   */
  public FreeTalkMainResponse {
    topics = List.copyOf(topics);
    followUps = List.copyOf(followUps);
  }

  /**
   * 무작위 추천 주제와 일일 발화 시간을 메인 응답으로 만든다.
   *
   * @param topics 무작위로 뽑은 활성 추천 주제(최대 5개)
   * @param dailySpeakingTimeLimitMs 일일 사용자 발화 시간 제한 밀리초
   * @param usedSpeakingTimeMs KST 당일 사용한 사용자 발화 시간 밀리초
   * @param remainingSpeakingTimeMs KST 당일 남은 사용자 발화 시간 밀리초
   * @param followUps 캐릭터별 사용 가능한 예고 질문
   * @return 프리톡 메인 응답
   */
  public static FreeTalkMainResponse of(
      List<FreeTalkTopicResponse> topics,
      long dailySpeakingTimeLimitMs,
      long usedSpeakingTimeMs,
      long remainingSpeakingTimeMs,
      List<FreeTalkAvailableFollowUp> followUps) {
    return new FreeTalkMainResponse(
        topics,
        dailySpeakingTimeLimitMs,
        usedSpeakingTimeMs,
        remainingSpeakingTimeMs,
        remainingSpeakingTimeMs > 0,
        followUps);
  }
}
