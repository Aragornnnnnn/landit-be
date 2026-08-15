// 다른 기능에 활성 TTS 음성 조회 경계를 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.TtsVoice;
import com.landit.landitbe.feature.content.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.content.repository.TtsVoiceRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 다른 기능에 활성 TTS 음성 조회 경계를 제공한다. */
@RequiredArgsConstructor
@Service
public class TtsVoiceService {

  private final TtsVoiceRepository ttsVoiceRepository;

  /**
   * Provider 음성 식별자에 대응하는 활성 음성을 조회한다.
   *
   * @param providerVoiceId Provider 음성 식별자
   * @return 활성 TTS 음성 응답
   * @throws ApiException 활성 음성이 없을 때
   */
  public TtsVoiceResponse requireActiveByProviderVoiceId(String providerVoiceId) {
    TtsVoice voice =
        ttsVoiceRepository
            .findByProviderVoiceIdAndStatus(providerVoiceId, ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    return TtsVoiceResponse.from(
        voice.getProvider(), voice.getModel(), voice.getProviderVoiceId(), voice.getGender());
  }
}
