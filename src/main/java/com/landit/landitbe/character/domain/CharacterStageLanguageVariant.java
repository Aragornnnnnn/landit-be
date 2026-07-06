// 캐릭터 단계의 기준 언어별 표시 이름을 저장한다.
package com.landit.landitbe.character.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "character_stage_language_variant")
public class CharacterStageLanguageVariant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_stage_id", nullable = false)
    private Long characterStageId;

    @Column(name = "base_locale", nullable = false, length = 35)
    private String baseLocale;

    @Column(nullable = false, length = 100)
    private String name;

    protected CharacterStageLanguageVariant() {
    }
}
