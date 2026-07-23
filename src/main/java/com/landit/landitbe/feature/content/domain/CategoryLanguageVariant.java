// 카테고리의 기준 언어별 표시 문구를 저장한다.

package com.landit.landitbe.feature.content.domain;

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

/** 카테고리의 기준 언어별 표시 문구를 저장한다. */
@Entity
@Table(name = "category_language_variant")
public class CategoryLanguageVariant extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "category_id", nullable = false)
  private Long categoryId;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Column(nullable = false, length = 100)
  private String name;

  /** 동작을 수행한다. */
  protected CategoryLanguageVariant() {}
}
