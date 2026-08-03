// 프리톡 세션별 표현 노출 순서를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 프리톡 세션별 표현 노출 순서를 저장한다. */
@Getter
@Entity
@Table(name = "free_talk_session_expression")
public class FreeTalkSessionExpression extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "free_talk_session_id", nullable = false)
  private Long freeTalkSessionId;

  @Column(name = "writing_expression_id", nullable = false)
  private Long writingExpressionId;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkSessionExpression() {}

  /**
   * Writing 표현을 프리톡 세션에 연결한다.
   *
   * @param freeTalkSessionId 연결할 프리톡 세션 ID
   * @param writingExpressionId 재사용할 Writing 표현 ID
   * @param displayOrder 세션 안의 노출 순서
   * @return 기존 표현을 참조하는 세션 표현
   */
  public static FreeTalkSessionExpression link(
      Long freeTalkSessionId, Long writingExpressionId, int displayOrder) {
    FreeTalkSessionExpression expression = new FreeTalkSessionExpression();
    expression.freeTalkSessionId = freeTalkSessionId;
    expression.writingExpressionId = writingExpressionId;
    expression.displayOrder = displayOrder;
    return expression;
  }
}
