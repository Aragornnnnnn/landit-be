// 시나리오 TTS 음성 설정 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.TtsVoice;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 시나리오 TTS 음성 설정 엔티티를 저장하고 조회한다. */
public interface TtsVoiceRepository extends JpaRepository<TtsVoice, Long> {

  /**
   * 상태가 일치하는 TTS 음성을 ID로 조회한다.
   *
   * @param id TTS 음성 ID
   * @param status 조회할 활성 상태
   * @return 조건에 맞는 TTS 음성, 없으면 빈 Optional
   */
  Optional<TtsVoice> findByIdAndStatus(Long id, ActiveStatus status);

  /**
   * 상태가 일치하는 TTS 음성을 Provider 음성 식별자로 조회한다.
   *
   * @param providerVoiceId Provider 음성 식별자
   * @param status 조회할 활성 상태
   * @return 조건에 맞는 TTS 음성, 없으면 빈 Optional
   */
  Optional<TtsVoice> findByProviderVoiceIdAndStatus(String providerVoiceId, ActiveStatus status);
}
