// TTS 음성 조회 서비스가 Repository 결과를 공개 응답 계약으로 변환하는지 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.domain.TtsVoice;
import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.feature.content.repository.TtsVoiceRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** TTS 음성 조회 서비스의 기능 간 공개 계약을 단위 검증한다. */
class TtsVoiceServiceTest {

  private final TtsVoiceRepository repository = mock(TtsVoiceRepository.class);
  private final TtsVoiceService service = new TtsVoiceService(repository);

  /** 활성 음성을 공개 응답 record로 변환한다. */
  @Test
  void returnsActiveVoiceByProviderVoiceId() {
    TtsVoice voice = mock(TtsVoice.class);
    when(voice.getProvider()).thenReturn(TtsVoiceProvider.OPENROUTER);
    when(voice.getModel()).thenReturn("deepgram/aura-2");
    when(voice.getProviderVoiceId()).thenReturn("aura-2-hyperion-en");
    when(voice.getGender()).thenReturn(TtsVoiceGender.MALE);
    when(repository.findByProviderVoiceIdAndStatus("aura-2-hyperion-en", ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(voice));

    assertThat(service.requireActiveByProviderVoiceId("aura-2-hyperion-en"))
        .extracting("provider", "model", "providerVoiceId", "gender")
        .containsExactly("OPENROUTER", "deepgram/aura-2", "aura-2-hyperion-en", "MALE");
  }

  /** 활성 음성이 없으면 리소스 없음 오류를 반환한다. */
  @Test
  void rejectsMissingActiveVoice() {
    when(repository.findByProviderVoiceIdAndStatus("missing", ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requireActiveByProviderVoiceId("missing"))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
