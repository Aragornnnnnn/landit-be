// 프리톡 세션별 개인화 예문과 표현 노출 순서를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 프리톡 세션별 개인화 예문과 표현 노출 순서를 저장한다. */
@Getter
@Entity
@Table(name = "free_talk_session_expression")
public class FreeTalkSessionExpression extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "free_talk_session_id", nullable = false)
  private Long freeTalkSessionId;

  @Column(name = "writing_expression_id")
  private Long writingExpressionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 20)
  private FreeTalkExpressionSourceType sourceType;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "personalized_example_text", nullable = false, columnDefinition = "text")
  private String personalizedExampleText;

  @Column(name = "personalized_example_translation", nullable = false, columnDefinition = "text")
  private String personalizedExampleTranslation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "generated_content_payload", columnDefinition = "jsonb")
  private JsonNode generatedContentPayload;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkSessionExpression() {}

  /** 기존 Writing 표현을 직접 참조하는 세션 표현을 생성한다. */
  public static FreeTalkSessionExpression existing(
      Long freeTalkSessionId,
      Long writingExpressionId,
      int displayOrder,
      String personalizedExampleText,
      String personalizedExampleTranslation) {
    FreeTalkSessionExpression expression = new FreeTalkSessionExpression();
    expression.freeTalkSessionId = freeTalkSessionId;
    expression.writingExpressionId = writingExpressionId;
    expression.sourceType = FreeTalkExpressionSourceType.EXISTING;
    expression.displayOrder = displayOrder;
    expression.personalizedExampleText = personalizedExampleText;
    expression.personalizedExampleTranslation = personalizedExampleTranslation;
    return expression;
  }

  /** AI가 생성한 학습 콘텐츠를 보유하는 세션 표현을 생성한다. */
  public static FreeTalkSessionExpression generated(
      Long freeTalkSessionId,
      int displayOrder,
      String personalizedExampleText,
      String personalizedExampleTranslation,
      JsonNode generatedContentPayload) {
    FreeTalkSessionExpression expression = new FreeTalkSessionExpression();
    expression.freeTalkSessionId = freeTalkSessionId;
    expression.sourceType = FreeTalkExpressionSourceType.NEW;
    expression.displayOrder = displayOrder;
    expression.personalizedExampleText = personalizedExampleText;
    expression.personalizedExampleTranslation = personalizedExampleTranslation;
    expression.generatedContentPayload = generatedContentPayload;
    return expression;
  }

  /** 학습 완료 시각을 기록한다. */
  public void complete(LocalDateTime completedAt) {
    if (this.completedAt == null) {
      this.completedAt = completedAt;
    }
  }

  /** 학습 완료 여부를 반환한다. */
  public boolean isCompleted() {
    return completedAt != null;
  }
}
