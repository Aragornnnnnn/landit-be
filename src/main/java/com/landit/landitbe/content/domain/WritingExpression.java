// 시나리오에 연결된 Writing 보충학습 표현 콘텐츠를 저장한다.
package com.landit.landitbe.content.domain;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.landit.landitbe.common.domain.Locale;

@Entity
@Getter
@Table(name = "writing_expression")
public class WritingExpression extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type", nullable = false, length = 30)
    private WritingExpressionType expressionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_frequency_level", nullable = false, length = 20)
    private ExpressionUsageFrequencyLevel usageFrequencyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_locale", nullable = false, length = 35)
    private Locale targetLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_locale", nullable = false, length = 35)
    private Locale baseLocale;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "target_expression_text", nullable = false, length = 255)
    private String targetExpressionText;

    @Column(name = "base_expression_meaning_text", nullable = false, length = 255)
    private String baseExpressionMeaningText;

    @Column(name = "usage_summary", nullable = false, length = 500)
    private String usageSummary;

    @Column(name = "usage_description", nullable = false, columnDefinition = "text")
    private String usageDescription;

    @Column(name = "representative_question_text", length = 500)
    private String representativeQuestionText;

    @Column(name = "representative_question_translation", length = 500)
    private String representativeQuestionTranslation;

    @Column(name = "representative_sentence_text", nullable = false, columnDefinition = "text")
    private String representativeSentenceText;

    @Column(name = "representative_sentence_translation", nullable = false, columnDefinition = "text")
    private String representativeSentenceTranslation;

    // 정답 예문을 단어 단위로 나눈 배열. 정답 판정용이라 순서를 그대로 유지한다.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "representative_sentence_words", nullable = false)
    private List<String> representativeSentenceWords;

    // 정답 단어 + 오답 단어를 섞은 선택지 배열. DB에 저장된 (섞인) 순서를 그대로 노출한다.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "representative_sentence_word_choices", nullable = false)
    private List<String> representativeSentenceWordChoices;

    @Column(name = "representative_image_url", length = 500)
    private String representativeImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "practice_examples_payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode practiceExamplesPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    protected WritingExpression() {
    }
}
