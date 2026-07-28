// 프리톡 세션에만 필요한 정보를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 프리톡 세션에만 필요한 정보를 저장한다. */
@Getter
@Entity
@Table(name = "free_talk_session")
public class FreeTalkSession extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "learning_session_id", nullable = false)
  private Long learningSessionId;

  @Column(name = "topic_id")
  private Long topicId;

  @Enumerated(EnumType.STRING)
  @Column(name = "start_mode", nullable = false, length = 20)
  private FreeTalkStartMode startMode;

  @Enumerated(EnumType.STRING)
  @Column(name = "conversation_status", nullable = false, length = 30)
  private FreeTalkConversationStatus conversationStatus;

  @Column(length = 255)
  private String title;

  @Column(name = "processing_client_message_id", length = 36)
  private String processingClientMessageId;

  @Column(name = "pending_user_message_id")
  private Long pendingUserMessageId;

  @Column(name = "accumulated_speaking_duration_ms", nullable = false)
  private long accumulatedSpeakingDurationMs;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkSession() {}

  private FreeTalkSession(Long learningSessionId, Long topicId, FreeTalkStartMode startMode) {
    this.learningSessionId = learningSessionId;
    this.topicId = topicId;
    this.startMode = startMode;
    this.conversationStatus = FreeTalkConversationStatus.IN_PROGRESS;
  }

  /**
   * 새 프리톡 세션 보조 정보를 생성한다.
   *
   * @param learningSessionId 연결할 학습 세션 ID
   * @param topicId 선택한 추천 주제 ID
   * @param startMode 첫 발화 주체
   * @return 진행 중 상태의 프리톡 세션
   */
  public static FreeTalkSession start(
      Long learningSessionId, Long topicId, FreeTalkStartMode startMode) {
    return new FreeTalkSession(learningSessionId, topicId, startMode);
  }

  /**
   * AI 선시작 프리톡의 선택 주제명을 세션 제목으로 기록한다.
   *
   * @param title 저장할 세션 제목
   */
  public void assignTitle(String title) {
    this.title = title;
  }

  /**
   * 종료 의사 확인이 필요한 상태로 전환한다.
   *
   * @throws IllegalStateException 진행 중 상태가 아닐 때
   */
  public void awaitExitDecision() {
    requireInProgress();
    conversationStatus = FreeTalkConversationStatus.AWAITING_EXIT_DECISION;
  }

  /**
   * 종료 의사 확인 대상 사용자 메시지를 기록한다.
   *
   * @param pendingUserMessageId 종료 확인 대상 사용자 메시지 ID
   * @throws IllegalStateException 진행 중 상태가 아닐 때
   */
  public void awaitExitDecision(long pendingUserMessageId) {
    awaitExitDecision();
    this.pendingUserMessageId = pendingUserMessageId;
  }

  /**
   * 외부 AI 처리 중인 사용자 발화 ID를 기록한다.
   *
   * @param clientMessageId 처리 중인 클라이언트 메시지 ID
   * @throws IllegalStateException 이미 다른 메시지를 처리 중일 때
   */
  public void startProcessing(String clientMessageId) {
    if (processingClientMessageId != null) {
      throw new IllegalStateException("처리 중인 사용자 메시지가 있습니다.");
    }
    processingClientMessageId = clientMessageId;
  }

  /** 외부 AI 처리 완료 또는 보상 후 처리 중인 사용자 발화 ID를 지운다. */
  public void clearProcessing() {
    processingClientMessageId = null;
  }

  /**
   * 종료 확인을 취소하고 대화를 계속한다.
   *
   * @throws IllegalStateException 종료 확인 대기 상태가 아닐 때
   */
  public void continueConversation() {
    if (conversationStatus != FreeTalkConversationStatus.AWAITING_EXIT_DECISION) {
      throw new IllegalStateException("종료 확인 대기 상태가 아닙니다.");
    }
    conversationStatus = FreeTalkConversationStatus.IN_PROGRESS;
    pendingUserMessageId = null;
  }

  /**
   * 사용자의 종료 확정으로 세션 대화를 완료한다.
   *
   * @throws IllegalStateException 이미 완료된 세션일 때
   */
  public void completeByUserExit() {
    if (conversationStatus == FreeTalkConversationStatus.COMPLETED) {
      throw new IllegalStateException("이미 완료된 프리톡 세션입니다.");
    }
    conversationStatus = FreeTalkConversationStatus.COMPLETED;
    pendingUserMessageId = null;
  }

  /**
   * 시간 제한 도달로 세션 대화를 완료한다.
   *
   * @throws IllegalStateException 진행 중 상태가 아닐 때
   */
  public void completeByTimeLimit() {
    requireInProgress();
    conversationStatus = FreeTalkConversationStatus.COMPLETED;
    pendingUserMessageId = null;
  }

  /**
   * 사용자 발화 시간을 누적한다.
   *
   * @param utteranceDurationMs 이번 사용자 발화 시간 밀리초
   * @throws IllegalStateException 진행 중 상태가 아니거나 시간이 음수일 때
   */
  public void addSpeakingDuration(long utteranceDurationMs) {
    requireInProgress();
    if (utteranceDurationMs < 0) {
      throw new IllegalStateException("사용자 발화 시간은 0 이상이어야 합니다.");
    }
    accumulatedSpeakingDurationMs += utteranceDurationMs;
  }

  private void requireInProgress() {
    if (conversationStatus != FreeTalkConversationStatus.IN_PROGRESS) {
      throw new IllegalStateException("진행 중인 프리톡 세션이 아닙니다.");
    }
  }
}
