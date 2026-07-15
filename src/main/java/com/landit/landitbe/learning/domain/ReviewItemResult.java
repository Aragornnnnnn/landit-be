// 복습 문항의 제출 결과를 저장한다.
package com.landit.landitbe.learning.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.common.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "review_item_result")
public class ReviewItemResult extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "review_item_id", nullable = false)
  private Long reviewItemId;

  @Column(name = "selected_answer", columnDefinition = "text")
  private String selectedAnswer;

  @Column(name = "submitted_answer", columnDefinition = "text")
  private String submittedAnswer;

  @Column(name = "is_correct", nullable = false)
  private boolean correct;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "answer_payload", columnDefinition = "jsonb")
  private JsonNode answerPayload;

  protected ReviewItemResult() {}
}
