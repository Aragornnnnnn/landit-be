// 시나리오 TTS 음성 설정 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.TtsVoice;
import org.springframework.data.jpa.repository.JpaRepository;

/** 시나리오 TTS 음성 설정 엔티티를 저장하고 조회한다. */
public interface TtsVoiceRepository extends JpaRepository<TtsVoice, Long> {}
