// 다른 기능에 공용 캐릭터의 활성 TTS 음성 조회 경계를 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.ConversationCharacter;
import com.landit.landitbe.feature.content.domain.TtsVoice;
import com.landit.landitbe.feature.content.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.content.repository.ConversationCharacterRepository;
import com.landit.landitbe.feature.content.repository.TtsVoiceRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 다른 기능에 공용 캐릭터의 활성 TTS 음성 조회 경계를 제공한다. */
@RequiredArgsConstructor
@Service
public class ConversationCharacterService {

  private final ConversationCharacterRepository conversationCharacterRepository;
  private final TtsVoiceRepository ttsVoiceRepository;

  /**
   * 캐릭터에 연결된 활성 TTS 음성을 조회한다.
   *
   * @param characterId 공개 캐릭터 식별자
   * @return 활성 TTS 음성 응답
   * @throws ApiException 활성 캐릭터 또는 음성이 없을 때
   */
  public TtsVoiceResponse requireActiveTtsVoice(String characterId) {
    ConversationCharacter character =
        conversationCharacterRepository
            .findByCharacterIdAndStatus(characterId, ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    TtsVoice voice =
        ttsVoiceRepository
            .findByIdAndStatus(character.getTtsVoiceId(), ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    return TtsVoiceResponse.from(
        voice.getProvider(), voice.getModel(), voice.getProviderVoiceId(), voice.getGender());
  }
}
