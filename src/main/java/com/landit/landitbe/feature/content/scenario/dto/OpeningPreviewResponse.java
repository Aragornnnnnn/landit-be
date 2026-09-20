// 시나리오 콘텐츠의 시작 메시지와 캐릭터 미리보기를 제공한다.

package com.landit.landitbe.feature.content.scenario.dto;

import com.landit.landitbe.feature.content.tutor.dto.ConversationCharacterResponse;
import com.landit.landitbe.feature.content.tutor.dto.TtsVoiceResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내부 타입을 정의한다.
 *
 * @param aiOpeningMessage AI first 시 첫 AI 메시지
 * @param aiOpeningMessageTranslation 첫 AI 메시지 번역
 * @param userOpeningInstruction USER first 시 사용자 시작 안내
 * @param innerThought 첫 화면에 보여줄 상대 역할의 속마음
 * @param innerThoughtType 속마음 유형
 * @param character 시나리오 캐릭터 정보
 */
@Schema(description = "시작 메시지 미리보기 응답")
public record OpeningPreviewResponse(
    @Schema(description = "AI first 시 첫 AI 메시지") String aiOpeningMessage,
    @Schema(description = "첫 AI 메시지 번역") String aiOpeningMessageTranslation,
    @Schema(description = "USER first 시 사용자 시작 안내") String userOpeningInstruction,
    @Schema(description = "첫 화면에 보여줄 상대 역할의 속마음") String innerThought,
    @Schema(description = "속마음 유형") String innerThoughtType,
    @Schema(description = "시나리오 캐릭터 정보") ConversationCharacterResponse character) {

  /**
   * AI가 먼저 발화하는 시나리오의 미리보기를 생성한다.
   *
   * @param projection 시나리오 조회 Projection
   * @return AI가 먼저 발화하는 시작 메시지 미리보기
   */
  public static OpeningPreviewResponse fromAi(ScenarioCatalogItem projection) {
    return new OpeningPreviewResponse(
        projection.aiOpeningMessage(),
        projection.aiOpeningMessageTranslation(),
        null,
        projection.innerThought(),
        projection.innerThoughtType() == null ? null : projection.innerThoughtType().name(),
        new ConversationCharacterResponse(
            projection.characterId(),
            TtsVoiceResponse.from(
                projection.ttsVoiceProvider(),
                projection.ttsVoiceModel(),
                projection.providerVoiceId(),
                projection.ttsVoiceGender())));
  }

  /**
   * 사용자가 먼저 발화하는 시나리오의 미리보기를 생성한다.
   *
   * @param projection 시나리오 조회 Projection
   * @return 사용자가 먼저 발화하는 시작 메시지 미리보기
   */
  public static OpeningPreviewResponse fromUser(ScenarioCatalogItem projection) {
    return new OpeningPreviewResponse(
        null,
        null,
        projection.userOpeningInstruction(),
        null,
        null,
        new ConversationCharacterResponse(
            projection.characterId(),
            TtsVoiceResponse.from(
                projection.ttsVoiceProvider(),
                projection.ttsVoiceModel(),
                projection.providerVoiceId(),
                projection.ttsVoiceGender())));
  }
}
