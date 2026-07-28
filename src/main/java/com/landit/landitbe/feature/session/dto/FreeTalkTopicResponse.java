// 프리톡 시작 화면에 노출할 추천 주제 응답을 정의한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.FreeTalkTopic;
import io.swagger.v3.oas.annotations.media.Schema;

/** 프리톡 시작 화면에 노출할 추천 주제 응답을 정의한다. */
@Schema(description = "프리톡 추천 주제")
public record FreeTalkTopicResponse(
    @Schema(description = "추천 주제 ID") Long topicId,
    @Schema(description = "화면 표시 주제명") String displayName,
    @Schema(description = "화면 노출 순서") int displayOrder) {

  /** 도메인 주제를 공개 응답으로 변환한다. */
  public static FreeTalkTopicResponse from(FreeTalkTopic topic) {
    return new FreeTalkTopicResponse(
        topic.getId(), topic.getDisplayName(), topic.getDisplayOrder());
  }
}
