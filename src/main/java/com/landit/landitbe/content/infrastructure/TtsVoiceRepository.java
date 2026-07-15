// 시나리오 TTS 음성 설정 엔티티를 저장하고 조회한다.
package com.landit.landitbe.content.infrastructure;

import com.landit.landitbe.content.domain.TtsVoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TtsVoiceRepository extends JpaRepository<TtsVoice, Long> {}
