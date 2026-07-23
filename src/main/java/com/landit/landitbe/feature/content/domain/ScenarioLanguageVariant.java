// 시나리오의 학습 언어와 기준 언어별 콘텐츠를 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import com.landit.landitbe.shared.domain.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 시나리오의 학습 언어와 기준 언어별 콘텐츠를 저장한다. */
@Entity
@Table(name = "scenario_language_variant")
public class ScenarioLanguageVariant extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "scenario_id", nullable = false)
  private Long scenarioId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_locale", nullable = false, length = 35)
  private Locale targetLocale;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String briefing;

  @Column(name = "user_opening_instruction", columnDefinition = "text")
  private String userOpeningInstruction;

  @Column(name = "conversation_goal", nullable = false, length = 255)
  private String conversationGoal;

  @Column(name = "tts_voice_id")
  private Long ttsVoiceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActiveStatus status;

  /** JPA에서 사용하는 기본 생성자다. */
  protected ScenarioLanguageVariant() {}
}
