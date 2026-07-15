// 사용자 메시지별 상세 피드백을 저장한다.
package com.landit.landitbe.session.domain;

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
import lombok.Getter;

@Getter
@Entity
@Table(name = "session_history_message_feedback")
public class SessionHistoryMessageFeedback extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_history_summary_feedback_id", nullable = false)
  private Long sessionHistorySummaryFeedbackId;

  @Column(name = "session_history_message_id", nullable = false)
  private Long sessionHistoryMessageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_locale", nullable = false, length = 35)
  private Locale targetLocale;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_locale", nullable = false, length = 35)
  private Locale baseLocale;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 20)
  private ProcessingStatus processingStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "feedback_type", length = 30)
  private FeedbackType feedbackType;

  @Column(name = "base_locale_analogy", columnDefinition = "text")
  private String baseLocaleAnalogy;

  @Column(name = "positive_feedback", columnDefinition = "text")
  private String positiveFeedback;

  @Column(name = "feedback_detail", columnDefinition = "text")
  private String feedbackDetail;

  @Column(name = "correction_expression", columnDefinition = "text")
  private String correctionExpression;

  @Column(name = "correction_reason", columnDefinition = "text")
  private String correctionReason;

  @Column(name = "benchmark_message", columnDefinition = "text")
  private String benchmarkMessage;

  protected SessionHistoryMessageFeedback() {}

  private SessionHistoryMessageFeedback(
      Long sessionHistorySummaryFeedbackId,
      Long sessionHistoryMessageId,
      Locale targetLocale,
      Locale baseLocale,
      ProcessingStatus processingStatus,
      FeedbackType feedbackType,
      String baseLocaleAnalogy,
      String positiveFeedback,
      String feedbackDetail,
      String correctionExpression,
      String correctionReason,
      String benchmarkMessage) {
    this.sessionHistorySummaryFeedbackId = sessionHistorySummaryFeedbackId;
    this.sessionHistoryMessageId = sessionHistoryMessageId;
    this.targetLocale = targetLocale;
    this.baseLocale = baseLocale;
    this.processingStatus = processingStatus;
    this.feedbackType = feedbackType;
    this.baseLocaleAnalogy = baseLocaleAnalogy;
    this.positiveFeedback = positiveFeedback;
    this.feedbackDetail = feedbackDetail;
    this.correctionExpression = correctionExpression;
    this.correctionReason = correctionReason;
    this.benchmarkMessage = benchmarkMessage;
  }

  /** AI 최종 피드백이 완료된 사용자 메시지 피드백을 생성한다. */
  public static SessionHistoryMessageFeedback completed(
      Long sessionHistorySummaryFeedbackId,
      Long sessionHistoryMessageId,
      Locale targetLocale,
      Locale baseLocale,
      FeedbackType feedbackType,
      String baseLocaleAnalogy,
      String positiveFeedback,
      String feedbackDetail,
      String correctionExpression,
      String correctionReason,
      String benchmarkMessage) {
    return new SessionHistoryMessageFeedback(
        sessionHistorySummaryFeedbackId,
        sessionHistoryMessageId,
        targetLocale,
        baseLocale,
        ProcessingStatus.COMPLETED,
        feedbackType,
        baseLocaleAnalogy,
        positiveFeedback,
        feedbackDetail,
        correctionExpression,
        correctionReason,
        benchmarkMessage);
  }
}
