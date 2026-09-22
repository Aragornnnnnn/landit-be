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
import java.time.LocalDateTime;
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
  private Long id; // 예: 9001

  @Column(name = "session_history_message_id", nullable = false, updatable = false)
  private Long sessionHistoryMessageId; // 예: 55020 (교정 대상 USER 메시지)

  @Column(name = "session_history_id", nullable = false, updatable = false)
  private Long sessionHistoryId; // 예: 3100 (그 메시지가 속한 대화 기록)

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 20)
  private ProcessingStatus processingStatus; // 예: COMPLETED

  @Column(name = "reacted_to_partner")
  private Boolean reactedToPartner; // 예: true. 판정에 실패하면 null

  // 예: "And I am doing stairs at a gym." (발화 전체가 아니라 그중 고른 한 문장)
  @Column(name = "original_sentence", columnDefinition = "text")
  private String originalSentence;

  // 예: "Today it's just stairs at the gym."
  @Column(name = "better_sentence", columnDefinition = "text")
  private String betterSentence;

  // 예: "9월 13일에 말한 그 헬스장이면 a gym이 아니라 the gym이에요."
  @Column(name = "reason", columnDefinition = "text")
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "mistake_pattern", length = 40)
  private FreeTalkMistakePattern mistakePattern; // 예: ARTICLE

  @Column(name = "memory_id")
  private Long memoryId; // 예: 9012. 기억을 근거로 쓰지 않았으면 null

  @Column(name = "memory_observed_on")
  private LocalDate memoryObservedOn; // 예: 2026-09-13 (그 기억을 말한 날)

  @Column(name = "memory_label", length = 40)
  private String memoryLabel; // 예: "헬스장". AI가 라벨을 못 주면 null

  @Column(name = "attempts", nullable = false)
  private int attempts; // 예: 1 (AI에 교정을 요청한 횟수. 이 컬럼이 생기기 전의 행은 0)

  // 애플리케이션 Clock(서울 시간)으로만 쓰고 비교한다. updated_at은 JVM 기본 시간대로 찍혀 기준으로 쓰지 않는다.
  @Column(name = "lease_until")
  private LocalDateTime leaseUntil; // 예: 2026-09-21T21:31:30. 판정이 끝났으면 null

  @Column(name = "attempt_token", length = 36)
  private String attemptToken; // 예: "0b9c…(UUID)". 복구 워커가 선점했을 때만 있고 첫 시도는 null

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkMessageFeedback() {}

  private FreeTalkMessageFeedback(
      Long sessionHistoryMessageId, Long sessionHistoryId, LocalDateTime leaseUntil) {
    this.sessionHistoryMessageId = sessionHistoryMessageId;
    this.sessionHistoryId = sessionHistoryId;
    this.processingStatus = ProcessingStatus.PREPARING;
    this.attempts = 1;
    this.leaseUntil = leaseUntil;
  }

  /**
   * AI의 교정 판정을 기다리는 행을 만든다.
   *
   * @param sessionHistoryMessageId 교정 대상 사용자 발화 ID
   * @param sessionHistoryId 그 발화가 속한 대화 기록 ID
   * @param leaseUntil 첫 시도의 응답을 기다려 줄 시각. 이 시각이 지나면 복구 워커가 넘겨받는다
   * @return 첫 시도를 시작한 준비 상태의 교정
   */
  public static FreeTalkMessageFeedback preparing(
      Long sessionHistoryMessageId, Long sessionHistoryId, LocalDateTime leaseUntil) {
    return new FreeTalkMessageFeedback(sessionHistoryMessageId, sessionHistoryId, leaseUntil);
  }

  /**
   * 이미 있는 행을 다시 판정을 기다리는 상태로 되돌린다.
   *
   * <p>교정 도입 전에 저장되어 실패로 채워진 발화가 도입 뒤에 확정되는 경우(예: 종료 확인을 기다리던 발화)에 쓴다. 한 번도 시도하지 않은 실패 행만 되돌린다. 준비
   * 상태의 교정은 문장 값을 가질 수 없다(chk_free_talk_message_feedback_sentence).
   *
   * @param leaseUntil 다시 시작한 첫 시도의 응답을 기다려 줄 시각
   */
  public void restartPreparing(LocalDateTime leaseUntil) {
    // 판정을 기다리는 중이거나 이미 끝난 교정은 다시 준비하지 않는다. 끝난 교정을 지우지 않기 위한 방어다.
    // 되돌리는 것은 교정을 한 번도 요청한 적 없이 실패로 채워진 행(attempts = 0)뿐이다. 실제로 시도하다 실패로 확정된 교정은
    // 한 번 끝나면 바뀌지 않아야 하므로 되살리지 않는다. 복구 워커는 준비 상태의 행만 쓰므로 이 쓰기와 겹치지 않는다.
    if (processingStatus != ProcessingStatus.FAILED || attempts != 0) {
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
    attempts = 1;
    this.leaseUntil = leaseUntil;
    attemptToken = null;
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
