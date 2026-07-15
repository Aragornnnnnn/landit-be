// 세션 히스토리 전체 피드백과 결과 요약을 저장한다.
package com.landit.landitbe.session.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
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

  protected SessionHistorySummaryFeedback() {}

  private SessionHistorySummaryFeedback(
      Long sessionHistoryId,
      ProcessingStatus processingStatus,
      int nativeScore,
      BigDecimal starRating,
      int totalMessageCount,
      int nativeLikeMessageCount,
      String highlightMessage,
      String summaryMessage) {
    this.sessionHistoryId = sessionHistoryId;
    this.processingStatus = processingStatus;
    this.nativeScore = nativeScore;
    this.starRating = starRating;
    this.totalMessageCount = totalMessageCount;
    this.nativeLikeMessageCount = nativeLikeMessageCount;
    this.highlightMessage = highlightMessage;
    this.summaryMessage = summaryMessage;
  }

  /** AI 최종 피드백이 완료된 세션 히스토리 요약을 생성한다. */
  public static SessionHistorySummaryFeedback completed(
      Long sessionHistoryId,
      int nativeScore,
      BigDecimal starRating,
      int totalMessageCount,
      int nativeLikeMessageCount,
      String highlightMessage,
      String summaryMessage) {
    return new SessionHistorySummaryFeedback(
        sessionHistoryId,
        ProcessingStatus.COMPLETED,
        nativeScore,
        starRating,
        totalMessageCount,
        nativeLikeMessageCount,
        highlightMessage,
        summaryMessage);
  }
}
