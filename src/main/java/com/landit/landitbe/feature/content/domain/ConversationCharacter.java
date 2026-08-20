// 시나리오와 프리톡이 공유하는 캐릭터와 TTS 음성 연결을 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 시나리오와 프리톡이 공유하는 캐릭터와 TTS 음성 연결을 저장한다. */
@Getter
@Entity
@Table(name = "conversation_character")
public class ConversationCharacter extends BaseTimeEntity {

  @Id
  @Column(name = "character_id", nullable = false, length = 20)
  private String characterId;

  @Column(name = "tts_voice_id", nullable = false)
  private Long ttsVoiceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActiveStatus status;

  /** JPA에서 사용하는 기본 생성자다. */
  protected ConversationCharacter() {}
}
