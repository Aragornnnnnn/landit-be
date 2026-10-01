// 프리톡 세션 시작 방식과 선택 주제를 받는다.

package com.landit.landitbe.feature.learning.freetalk.start.dto;

import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkStartMode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프리톡 세션 시작 방식과 선택 주제를 받는다.
 *
 * @param startMode 첫 발화 주체
 * @param topicId AI 선시작에서 선택한 추천 주제 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param followUpId AI 선시작에서 이어갈 예고 질문 ID
 */
@Schema(description = "프리톡 세션 시작 요청")
public record FreeTalkSessionStartRequest(
    @Schema(description = "첫 발화 주체", example = "AI_FIRST") FreeTalkStartMode startMode,
    @Schema(description = "AI 선시작에서 선택한 활성 추천 주제 ID", example = "2") Long topicId,
    @Schema(
            description = "프리톡 캐릭터 식별자",
            example = "chloe",
            allowableValues = {"chloe", "marco", "teddy"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String characterId,
    @Schema(description = "AI 선시작에서 이어갈 예고 질문 ID", example = "123") Long followUpId) {

  /**
   * 기존 주제 선택과 사용자 선시작 요청을 유지한다.
   *
   * @param startMode 첫 발화 주체
   * @param topicId 추천 주제 ID
   * @param characterId 캐릭터 ID
   */
  public FreeTalkSessionStartRequest(
      FreeTalkStartMode startMode, Long topicId, String characterId) {
    this(startMode, topicId, characterId, null);
  }

  /**
   * 기존 내부 호출부가 기본 캐릭터로 요청을 생성한다.
   *
   * @param startMode 첫 발화 주체
   * @param topicId 추천 주제 ID
   */
  public FreeTalkSessionStartRequest(FreeTalkStartMode startMode, Long topicId) {
    this(startMode, topicId, "chloe", null);
  }
}
