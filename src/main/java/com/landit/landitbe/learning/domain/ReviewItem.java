// 자정 스케줄링으로 생성된 복습 문항을 저장한다.

package com.landit.landitbe.learning.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.common.domain.BaseTimeEntity;
import com.landit.landitbe.common.domain.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 자정 스케줄링으로 생성된 복습 문항을 저장한다. */
@Entity
@Table(name = "review_item")
public class ReviewItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "user_learning_expression_id", nullable = false)
  private Long userLearningExpressionId;

  @Column(name = "session_history_message_feedback_id", nullable = false)
  private Long sessionHistoryMessageFeedbackId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_locale", nullable = false, length = 35)
  private Locale targetLocale;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Column(name = "review_date", nullable = false)
  private LocalDate reviewDate;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "quiz_type", nullable = false, length = 40)
  private QuizType quizType;

  @Column(name = "question_text", nullable = false, columnDefinition = "text")
  private String questionText;

  @Column(name = "correct_answer", columnDefinition = "text")
  private String correctAnswer;

  @Column(name = "explanation_text", columnDefinition = "text")
  private String explanationText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "quiz_payload", nullable = false, columnDefinition = "jsonb")
  private JsonNode quizPayload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReviewItemStatus status;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  /** 동작을 수행한다. */
  protected ReviewItem() {}
}
