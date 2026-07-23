// 시나리오 상대 역할에 사용할 TTS 음성 설정을 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 시나리오 상대 역할에 사용할 TTS 음성 설정을 저장한다. */
@Getter
@Entity
@Table(name = "tts_voice")
public class TtsVoice extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TtsVoiceProvider provider;

  @Column(nullable = false, length = 100)
  private String model;

  @Column(name = "provider_voice_id", nullable = false, length = 150)
  private String providerVoiceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TtsVoiceGender gender;

  @Column(length = 255)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "accent_locale", nullable = false, length = 35)
  private AccentLocale accentLocale;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActiveStatus status;

  /** 동작을 수행한다. */
  protected TtsVoice() {}
}
