// 프리톡에서 재사용하거나 새로 생성한 공통 표현 학습 콘텐츠를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.fasterxml.jackson.databind.JsonNode;
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
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 프리톡에서 재사용하거나 새로 생성한 공통 표현 학습 콘텐츠를 저장한다. */
@Getter
@Entity
@Table(name = "free_talk_expression")
public class FreeTalkExpression extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "writing_expression_id")
  private Long writingExpressionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 20)
  private FreeTalkExpressionSourceType sourceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_locale", nullable = false, length = 35)
  private Locale targetLocale;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Column(name = "target_expression_text", nullable = false)
  private String targetExpressionText;

  @Column(name = "base_expression_meaning_text", nullable = false)
  private String baseExpressionMeaningText;

  @Column(name = "usage_summary", nullable = false)
  private String usageSummary;

  @Column(name = "usage_description", columnDefinition = "text")
  private String usageDescription;

  @Column(name = "representative_question_text")
  private String representativeQuestionText;

  @Column(name = "representative_question_translation")
  private String representativeQuestionTranslation;

  @Column(name = "representative_sentence_text", columnDefinition = "text")
  private String representativeSentenceText;

  @Column(name = "representative_sentence_translation", columnDefinition = "text")
  private String representativeSentenceTranslation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "representative_sentence_words", columnDefinition = "jsonb")
  private JsonNode representativeSentenceWords;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "representative_sentence_word_choices", columnDefinition = "jsonb")
  private JsonNode representativeSentenceWordChoices;

  @Column(name = "representative_image_url")
  private String representativeImageUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "practice_examples_payload", columnDefinition = "jsonb")
  private JsonNode practiceExamplesPayload;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkExpression() {}

  /**
   * 기존 Writing 표현을 참조하는 프리톡 표현을 생성한다.
   *
   * @param writingExpressionId 참조할 Writing 표현 ID
   * @param targetLocale 학습 대상 locale
   * @param baseLocale 기준 locale
   * @param targetExpressionText 학습 표현 원문
   * @param baseExpressionMeaningText 기준 언어 뜻
   * @param usageSummary 짧은 사용 요약
   * @return 기존 표현을 참조하는 프리톡 표현
   */
  public static FreeTalkExpression existingExpression(
      Long writingExpressionId,
      Locale targetLocale,
      Locale baseLocale,
      String targetExpressionText,
      String baseExpressionMeaningText,
      String usageSummary) {
    FreeTalkExpression expression = new FreeTalkExpression();
    expression.writingExpressionId = writingExpressionId;
    expression.sourceType = FreeTalkExpressionSourceType.EXISTING;
    expression.targetLocale = targetLocale;
    expression.baseLocale = baseLocale;
    expression.targetExpressionText = targetExpressionText;
    expression.baseExpressionMeaningText = baseExpressionMeaningText;
    expression.usageSummary = usageSummary;
    return expression;
  }

  /**
   * AI-5가 생성한 신규 프리톡 표현 학습 콘텐츠를 생성한다.
   *
   * @param targetLocale 학습 대상 locale
   * @param baseLocale 기준 locale
   * @param targetExpressionText 학습 표현 원문
   * @param baseExpressionMeaningText 기준 언어 뜻
   * @param usageSummary 짧은 사용 요약
   * @param usageDescription 상세 사용 설명
   * @param representativeQuestionText 대표 예문 질문
   * @param representativeQuestionTranslation 대표 질문 번역
   * @param representativeSentenceText 대표 예문 원문
   * @param representativeSentenceTranslation 대표 예문 번역
   * @param representativeSentenceWords 대표 예문 단어 배열
   * @param representativeSentenceWordChoices 대표 예문 선택지 배열
   * @param representativeImageUrl 대표 이미지 URL
   * @param practiceExamplesPayload 추가 예문 JSON 배열
   * @return 신규 표현 콘텐츠
   */
  public static FreeTalkExpression newExpression(
      Locale targetLocale,
      Locale baseLocale,
      String targetExpressionText,
      String baseExpressionMeaningText,
      String usageSummary,
      String usageDescription,
      String representativeQuestionText,
      String representativeQuestionTranslation,
      String representativeSentenceText,
      String representativeSentenceTranslation,
      JsonNode representativeSentenceWords,
      JsonNode representativeSentenceWordChoices,
      String representativeImageUrl,
      JsonNode practiceExamplesPayload) {
    FreeTalkExpression expression = new FreeTalkExpression();
    expression.sourceType = FreeTalkExpressionSourceType.NEW;
    expression.targetLocale = targetLocale;
    expression.baseLocale = baseLocale;
    expression.targetExpressionText = targetExpressionText;
    expression.baseExpressionMeaningText = baseExpressionMeaningText;
    expression.usageSummary = usageSummary;
    expression.usageDescription = usageDescription;
    expression.representativeQuestionText = representativeQuestionText;
    expression.representativeQuestionTranslation = representativeQuestionTranslation;
    expression.representativeSentenceText = representativeSentenceText;
    expression.representativeSentenceTranslation = representativeSentenceTranslation;
    expression.representativeSentenceWords = representativeSentenceWords;
    expression.representativeSentenceWordChoices = representativeSentenceWordChoices;
    expression.representativeImageUrl = representativeImageUrl;
    expression.practiceExamplesPayload = practiceExamplesPayload;
    return expression;
  }
}
