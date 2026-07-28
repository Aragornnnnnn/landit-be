// 시나리오에 연결된 Writing 보충학습 표현 콘텐츠를 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 시나리오에 연결된 Writing 보충학습 표현 콘텐츠를 저장한다. */
@Entity
@Getter
@Table(name = "writing_expression")
public class WritingExpression extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "scenario_id")
  private Long scenarioId;

  @Column(name = "owner_user_profile_id")
  private Long ownerUserProfileId;

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

  /** JPA에서 사용하는 기본 생성자다. */
  protected WritingExpression() {}

  /**
   * 프리톡에서 생성한 사용자 전용 학습 표현을 만든다.
   *
   * @param ownerUserProfileId 표현을 소유할 사용자 ID
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param content 생성한 학습 콘텐츠
   * @return 활성 상태의 사용자 전용 표현
   */
  public static WritingExpression freeTalkGenerated(
      Long ownerUserProfileId,
      Locale targetLocale,
      Locale baseLocale,
      FreeTalkGeneratedExpressionContent content) {
    WritingExpression expression = new WritingExpression();
    expression.ownerUserProfileId = ownerUserProfileId;
    expression.expressionType = WritingExpressionType.CONVERSATION_SKILL;
    expression.usageFrequencyLevel = ExpressionUsageFrequencyLevel.BASIC;
    expression.targetLocale = targetLocale;
    expression.baseLocale = baseLocale;
    expression.displayOrder = 1;
    expression.targetExpressionText = content.targetExpressionText();
    expression.baseExpressionMeaningText = content.baseExpressionMeaningText();
    expression.usageSummary = content.usageSummary();
    expression.usageDescription = content.usageDescription();
    expression.representativeQuestionText = content.representativeQuestionText();
    expression.representativeQuestionTranslation = content.representativeQuestionTranslation();
    expression.representativeSentenceText = content.representativeSentenceText();
    expression.representativeSentenceTranslation = content.representativeSentenceTranslation();
    expression.representativeSentenceWords = content.representativeSentenceWords();
    expression.representativeSentenceWordChoices = content.representativeSentenceWordChoices();
    expression.representativeImageUrl = content.representativeImageUrl();
    expression.practiceExamplesPayload = content.practiceExamplesPayload();
    expression.status = ActiveStatus.ACTIVE;
    return expression;
  }

  /** 사용자 전용 표현을 다른 사용자가 조회하려는지 확인한다. */
  public boolean isOwnedByAnother(Long userProfileId) {
    return ownerUserProfileId != null && !ownerUserProfileId.equals(userProfileId);
  }
}
