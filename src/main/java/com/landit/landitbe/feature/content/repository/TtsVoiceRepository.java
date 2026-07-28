// 시나리오 TTS 음성 설정 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.TtsVoice;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 시나리오 TTS 음성 설정 엔티티를 저장하고 조회한다. */
public interface TtsVoiceRepository extends JpaRepository<TtsVoice, Long> {

  /** 활성 상태의 TTS 음성을 ID로 조회한다. */
  Optional<TtsVoice> findByIdAndStatus(Long id, ActiveStatus status);
}
