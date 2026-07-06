// 퀘스트 원형의 기준 언어별 표시 문구를 저장한다.
package com.landit.landitbe.quest.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quest_template_language_variant")
public class QuestTemplateLanguageVariant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quest_template_id", nullable = false)
    private Long questTemplateId;

    @Column(name = "base_locale", nullable = false, length = 35)
    private String baseLocale;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    protected QuestTemplateLanguageVariant() {
    }
}
