// 프리톡 세션별 개인화 예문과 표현 노출 순서를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

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

  @Column(name = "free_talk_expression_id", nullable = false)
  private Long freeTalkExpressionId;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "personalized_example_text", nullable = false, columnDefinition = "text")
  private String personalizedExampleText;

  @Column(name = "personalized_example_translation", nullable = false, columnDefinition = "text")
  private String personalizedExampleTranslation;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkSessionExpression() {}

  /** 세션의 맞춤 표현 연결과 개인화 예문을 생성한다. */
  public static FreeTalkSessionExpression create(
      Long freeTalkSessionId,
      Long freeTalkExpressionId,
      int displayOrder,
      String personalizedExampleText,
      String personalizedExampleTranslation) {
    FreeTalkSessionExpression expression = new FreeTalkSessionExpression();
    expression.freeTalkSessionId = freeTalkSessionId;
    expression.freeTalkExpressionId = freeTalkExpressionId;
    expression.displayOrder = displayOrder;
    expression.personalizedExampleText = personalizedExampleText;
    expression.personalizedExampleTranslation = personalizedExampleTranslation;
    return expression;
  }
}
