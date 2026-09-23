// 프리톡 시작 화면에 노출할 추천 주제 응답을 정의한다.

package com.landit.landitbe.feature.learning.freetalk.topic.dto;

import com.landit.landitbe.feature.learning.freetalk.topic.domain.FreeTalkTopic;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프리톡 시작 화면에 노출할 추천 주제 응답을 정의한다.
 *
 * @param topicId 추천 주제 ID
 * @param displayName 화면 표시 주제명
 * @param displayOrder 이번 응답 안에서의 노출 순서(1부터). DB의 display_order가 아니라 무작위로 뽑힌 순서다.
 */
@Schema(description = "프리톡 추천 주제")
public record FreeTalkTopicResponse(
    @Schema(description = "추천 주제 ID") Long topicId,
    @Schema(description = "화면 표시 주제명") String displayName,
    @Schema(description = "이번 응답 안에서의 노출 순서(1부터). 요청마다 달라진다.") int displayOrder) {

  /**
   * 도메인 주제를 이번 응답의 노출 순서와 함께 공개 응답으로 변환한다.
   *
   * @param topic 변환할 프리톡 주제
   * @param displayOrder 이번 응답 안에서의 노출 순서(1부터)
   * @return 공개 주제 응답
   */
  public static FreeTalkTopicResponse of(FreeTalkTopic topic, int displayOrder) {
    return new FreeTalkTopicResponse(topic.getId(), topic.getDisplayName(), displayOrder);
  }
}
