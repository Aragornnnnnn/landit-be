// 프리톡 사용자 발화 한 턴의 교정을 대화 메시지와 분리해 저장한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.domain;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;

/**
 * 프리톡 사용자 발화 한 턴의 교정이다.
 *
 * <p>교정 대상인 사용자 발화에만 행이 있고 한 발화에 한 행만 둔다. AI 메시지와 시나리오 발화에는 행이 없다.
 */
@Getter
@Entity
@Table(name = "free_talk_message_feedback")
public class FreeTalkMessageFeedback extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_history_message_id", nullable = false, updatable = false)
  private Long sessionHistoryMessageId;

  @Column(name = "session_history_id", nullable = false, updatable = false)
  private Long sessionHistoryId;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 20)
  private ProcessingStatus processingStatus;

  @Column(name = "reacted_to_partner")
  private Boolean reactedToPartner;

  @Column(name = "original_sentence", columnDefinition = "text")
  private String originalSentence;

  @Column(name = "better_sentence", columnDefinition = "text")
  private String betterSentence;

  @Column(name = "reason", columnDefinition = "text")
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "mistake_pattern", length = 40)
  private FreeTalkMistakePattern mistakePattern;

  @Column(name = "memory_id")
  private Long memoryId;

  @Column(name = "memory_observed_on")
  private LocalDate memoryObservedOn;

  @Column(name = "memory_label", length = 40)
  private String memoryLabel;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkMessageFeedback() {}

  private FreeTalkMessageFeedback(Long sessionHistoryMessageId, Long sessionHistoryId) {
    this.sessionHistoryMessageId = sessionHistoryMessageId;
    this.sessionHistoryId = sessionHistoryId;
    this.processingStatus = ProcessingStatus.PREPARING;
  }

  /**
   * AI의 교정 판정을 기다리는 행을 만든다.
   *
   * @param sessionHistoryMessageId 교정 대상 사용자 발화 ID
   * @param sessionHistoryId 그 발화가 속한 대화 기록 ID
   * @return 준비 상태의 교정
   */
  public static FreeTalkMessageFeedback preparing(
      Long sessionHistoryMessageId, Long sessionHistoryId) {
    return new FreeTalkMessageFeedback(sessionHistoryMessageId, sessionHistoryId);
  }

  /**
   * 이미 있는 행을 다시 판정을 기다리는 상태로 되돌린다.
   *
   * <p>교정 도입 전에 저장되어 실패로 채워진 발화가 도입 뒤에 확정되는 경우(예: 종료 확인을 기다리던 발화)에 쓴다. 실패 상태의 행만 되돌린다. 준비 상태의 교정은
   * 문장 값을 가질 수 없다(chk_free_talk_message_feedback_sentence).
   */
  public void restartPreparing() {
    // 판정을 기다리는 중이거나 이미 끝난 교정은 다시 준비하지 않는다. 끝난 교정을 지우지 않기 위한 방어다.
    if (processingStatus != ProcessingStatus.FAILED) {
      return;
    }
    processingStatus = ProcessingStatus.PREPARING;
    reactedToPartner = null;
    originalSentence = null;
    betterSentence = null;
    reason = null;
    mistakePattern = null;
    memoryId = null;
    memoryObservedOn = null;
    memoryLabel = null;
  }

  /**
   * 저장된 값을 교정 판정 결과로 돌려준다.
   *
   * @return 고칠 것이 없거나 생성 중·실패면 문장이 null인 판정 결과
   */
  public FreeTalkTurnCorrection toCorrection() {
    return new FreeTalkTurnCorrection(processingStatus, sentence(), reactedToPartner);
  }

  /**
   * 근거 기억 값의 짝이 맞는지 돌려준다. 근거 기억과 날짜는 함께 있거나 함께 없고, 라벨은 근거 기억이 있을 때만 있다.
   *
   * @return DB 제약(chk_free_talk_message_feedback_memory)과 같은 규칙을 만족하면 true
   */
  public boolean hasConsistentMemory() {
    return (memoryId == null) == (memoryObservedOn == null)
        && (memoryId != null || memoryLabel == null);
  }

  // 지난 기록을 읽는 길이라 근거 기억 값이 어긋난 행이 있어도 세션 상세 전체를 막지 않고 근거 기억만 뺀다.
  private FreeTalkTurnCorrection.Sentence sentence() {
    if (betterSentence == null) {
      return null;
    }
    if (!hasConsistentMemory()) {
      return new FreeTalkTurnCorrection.Sentence(
          originalSentence, betterSentence, reason, mistakePattern);
    }
    return new FreeTalkTurnCorrection.Sentence(
        originalSentence,
        betterSentence,
        reason,
        mistakePattern,
        memoryId,
        memoryObservedOn,
        memoryLabel);
  }
}
