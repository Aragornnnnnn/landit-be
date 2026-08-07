// 캐릭터 단계의 기준 언어별 표시 이름을 저장한다.

package com.landit.landitbe.feature.character.domain;

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

/** 캐릭터 단계의 기준 언어별 표시 이름을 저장한다. */
@Entity
@Table(name = "character_stage_language_variant")
public class CharacterStageLanguageVariant extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "character_stage_id", nullable = false)
  private Long characterStageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Column(nullable = false, length = 100)
  private String name;

  /** JPA에서 사용하는 기본 생성자다. */
  protected CharacterStageLanguageVariant() {}
}
