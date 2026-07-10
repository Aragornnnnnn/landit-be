// 시나리오 고정 질문의 학습 언어와 기준 언어별 문구를 저장한다.
package com.landit.landitbe.content.domain;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import com.landit.landitbe.common.domain.Locale;

@Getter
@Entity
@Table(name = "scenario_question_language_variant")
public class ScenarioQuestionLanguageVariant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_question_id", nullable = false)
    private Long scenarioQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_locale", nullable = false, length = 35)
    private Locale targetLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_locale", nullable = false, length = 35)
    private Locale baseLocale;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(name = "question_translation", nullable = false, length = 500)
    private String questionTranslation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    protected ScenarioQuestionLanguageVariant() {
    }
}
