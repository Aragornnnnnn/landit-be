// 퀘스트 원형의 기준 언어별 표시 문구를 저장한다.

package com.landit.landitbe.feature.quest.domain;

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

/** 퀘스트 원형의 기준 언어별 표시 문구를 저장한다. */
@Entity
@Table(name = "quest_template_language_variant")
public class QuestTemplateLanguageVariant extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "quest_template_id", nullable = false)
  private Long questTemplateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  /** JPA에서 사용하는 기본 생성자다. */
  protected QuestTemplateLanguageVariant() {}
}
