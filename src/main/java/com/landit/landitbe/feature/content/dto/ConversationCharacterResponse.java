// 시나리오와 프리톡의 대화 캐릭터 정보를 공통 응답으로 제공한다.

package com.landit.landitbe.feature.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시나리오와 프리톡의 대화 캐릭터 정보를 제공한다.
 *
 * @param characterId 캐릭터 식별자
 * @param ttsVoice 활성 TTS 음성. 미설정 또는 비활성 음성이면 null
 */
@Schema(description = "대화 캐릭터 정보")
public record ConversationCharacterResponse(
    @Schema(description = "캐릭터 식별자", example = "chloe") String characterId,
    @Schema(
            description = "활성 TTS 음성. 미설정 또는 비활성 음성이면 null",
            nullable = true,
            types = {"object", "null"})
        TtsVoiceResponse ttsVoice) {}
