// 공용 캐릭터 서비스가 DB의 캐릭터와 활성 TTS 음성을 연결하는지 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.domain.ConversationCharacter;
import com.landit.landitbe.feature.content.domain.TtsVoice;
import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.feature.content.repository.ConversationCharacterRepository;
import com.landit.landitbe.feature.content.repository.TtsVoiceRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 공용 캐릭터의 기능 간 TTS 조회 계약을 단위 검증한다. */
class ConversationCharacterServiceTest {

  private final ConversationCharacterRepository characterRepository =
      mock(ConversationCharacterRepository.class);
  private final TtsVoiceRepository ttsVoiceRepository = mock(TtsVoiceRepository.class);
  private final ConversationCharacterService service =
      new ConversationCharacterService(characterRepository, ttsVoiceRepository);

  /** 캐릭터에 연결된 활성 음성을 공개 응답 record로 변환한다. */
  @Test
  void returnsActiveTtsVoiceForCharacter() {
    ConversationCharacter character = mock(ConversationCharacter.class);
    TtsVoice voice = mock(TtsVoice.class);
    when(character.getTtsVoiceId()).thenReturn(2L);
    when(characterRepository.findByCharacterIdAndStatus("marco", ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(character));
    when(ttsVoiceRepository.findByIdAndStatus(2L, ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(voice));
    when(voice.getProvider()).thenReturn(TtsVoiceProvider.OPENROUTER);
    when(voice.getModel()).thenReturn("deepgram/aura-2");
    when(voice.getProviderVoiceId()).thenReturn("aura-2-hyperion-en");
    when(voice.getGender()).thenReturn(TtsVoiceGender.MALE);

    assertThat(service.requireActiveTtsVoice("marco"))
        .extracting("provider", "model", "providerVoiceId", "gender")
        .containsExactly("OPENROUTER", "deepgram/aura-2", "aura-2-hyperion-en", "MALE");
  }

  /** 등록된 활성 캐릭터가 없으면 리소스 없음 오류를 반환한다. */
  @Test
  void rejectsMissingActiveCharacter() {
    when(characterRepository.findByCharacterIdAndStatus("unknown", ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requireActiveTtsVoice("unknown"))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
