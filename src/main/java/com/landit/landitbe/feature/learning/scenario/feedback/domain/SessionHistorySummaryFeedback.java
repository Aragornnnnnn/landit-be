// 세션 히스토리 전체 피드백과 결과 요약을 저장한다.

package com.landit.landitbe.feature.learning.scenario.feedback.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 세션 히스토리 전체 피드백과 결과 요약을 저장한다. */
@Getter
@Entity
@Table(name = "session_history_summary_feedback")
public class SessionHistorySummaryFeedback extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_history_id", nullable = false)
  private Long sessionHistoryId;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 20)
  private ProcessingStatus processingStatus;

  @Column(name = "native_score")
  private Integer nativeScore;

  @Column(name = "star_rating")
  private BigDecimal starRating;

  @Column(name = "total_message_count")
  private Integer totalMessageCount;

  @Column(name = "native_like_message_count")
  private Integer nativeLikeMessageCount;

  @Column(name = "highlight_message", columnDefinition = "text")
  private String highlightMessage;

  @Column(name = "summary_message", columnDefinition = "text")
  private String summaryMessage;

  @Column(name = "growth_feedback_payload", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode growthFeedbackPayload;

  @Column(name = "expression_reuse_payload", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode expressionReusePayload;

  /** JPA에서 사용하는 기본 생성자다. */
  protected SessionHistorySummaryFeedback() {}

  private SessionHistorySummaryFeedback(
      Long sessionHistoryId,
      ProcessingStatus processingStatus,
      int nativeScore,
      BigDecimal starRating,
      int totalMessageCount,
      int nativeLikeMessageCount,
      String highlightMessage,
      String summaryMessage,
      JsonNode growthFeedbackPayload,
      JsonNode expressionReusePayload) {
    this.sessionHistoryId = sessionHistoryId;
    this.processingStatus = processingStatus;
    this.nativeScore = nativeScore;
    this.starRating = starRating;
    this.totalMessageCount = totalMessageCount;
    this.nativeLikeMessageCount = nativeLikeMessageCount;
    this.highlightMessage = highlightMessage;
    this.summaryMessage = summaryMessage;
    this.growthFeedbackPayload = growthFeedbackPayload;
    this.expressionReusePayload = expressionReusePayload;
  }

  /**
   * AI 최종 피드백이 완료된 세션 히스토리 요약을 생성한다.
   *
   * @param sessionHistoryId 세션 히스토리 ID
   * @param nativeScore 원어민 관점 점수
   * @param starRating 세션 별점
   * @param totalMessageCount 사용자 메시지 수
   * @param nativeLikeMessageCount 원어민처럼 평가된 메시지 수
   * @param highlightMessage 최종 피드백 강조 메시지
   * @param summaryMessage 최종 피드백 요약
   * @param growthFeedbackPayload 직전 교정 비교 결과
   * @param expressionReusePayload 배운 표현 재사용 분석 결과
   * @return 완료 상태의 최종 피드백 요약
   */
  public static SessionHistorySummaryFeedback completed(
      Long sessionHistoryId,
      int nativeScore,
      BigDecimal starRating,
      int totalMessageCount,
      int nativeLikeMessageCount,
      String highlightMessage,
      String summaryMessage,
      JsonNode growthFeedbackPayload,
      JsonNode expressionReusePayload) {
    return new SessionHistorySummaryFeedback(
        sessionHistoryId,
        ProcessingStatus.COMPLETED,
        nativeScore,
        starRating,
        totalMessageCount,
        nativeLikeMessageCount,
        highlightMessage,
        summaryMessage,
        growthFeedbackPayload,
        expressionReusePayload);
  }
}
