// 시나리오 상대 역할의 TTS 음성 정보를 API 응답으로 제공한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시나리오 상대 역할의 TTS 음성 정보를 API 응답으로 제공한다.
 *
 * @param provider TTS Provider
 * @param model TTS 모델
 * @param providerVoiceId Provider에서 사용하는 음성 ID
 * @param gender 음성 성별
 */
@Schema(description = "시나리오 TTS 음성")
public record TtsVoiceResponse(
    @Schema(description = "TTS Provider") String provider,
    @Schema(description = "TTS 모델") String model,
    @Schema(description = "Provider에서 사용하는 음성 ID") String providerVoiceId,
    @Schema(description = "음성 성별") String gender) {

  /**
   * 활성 TTS 음성 조회 결과를 응답으로 변환하고 미설정 또는 비활성 결과는 null로 유지한다.
   *
   * @param provider TTS Provider
   * @param model TTS 모델
   * @param providerVoiceId Provider에서 사용하는 음성 ID
   * @param gender 음성 성별
   * @return TTS 음성 응답. Provider가 없으면 null
   */
  public static TtsVoiceResponse from(
      TtsVoiceProvider provider, String model, String providerVoiceId, TtsVoiceGender gender) {
    if (provider == null) {
      return null;
    }
    return new TtsVoiceResponse(provider.name(), model, providerVoiceId, gender.name());
  }
}
