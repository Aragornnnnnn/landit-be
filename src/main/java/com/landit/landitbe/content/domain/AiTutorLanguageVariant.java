// AI 튜터의 기준 언어별 표시 이름을 저장한다.
package com.landit.landitbe.content.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.landit.landitbe.common.domain.Locale;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "ai_tutor_language_variant")
public class AiTutorLanguageVariant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ai_tutor_id", nullable = false)
    private Long aiTutorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_locale", nullable = false, length = 35)
    private Locale baseLocale;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    protected AiTutorLanguageVariant() {
    }
}
