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
import java.time.LocalDateTime;
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

  @Column(name = "character_id", nullable = false, length = 20)
  private String characterId;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "expression_generation_status", length = 20)
  private ExpressionGenerationStatus expressionGenerationStatus;

  @Column(name = "expression_generation_started_at")
  private LocalDateTime expressionGenerationStartedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "memory_generation_status", length = 20)
  private MemoryGenerationStatus memoryGenerationStatus;

  @Column(name = "memory_generation_started_at")
  private LocalDateTime memoryGenerationStartedAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkSession() {}

  private FreeTalkSession(
      Long learningSessionId,
      Long topicId,
      FreeTalkStartMode startMode,
      FreeTalkCharacter character) {
    this.learningSessionId = learningSessionId;
    this.topicId = topicId;
    this.startMode = startMode;
    this.characterId = character.id();
    this.conversationStatus = FreeTalkConversationStatus.IN_PROGRESS;
  }

  /**
   * 새 프리톡 세션 보조 정보를 생성한다.
   *
   * @param learningSessionId 연결할 학습 세션 ID
   * @param topicId 선택한 추천 주제 ID
   * @param startMode 첫 발화 주체
   * @param character 선택한 대화 상대 캐릭터
   * @return 진행 중 상태의 프리톡 세션
   */
  public static FreeTalkSession start(
      Long learningSessionId,
      Long topicId,
      FreeTalkStartMode startMode,
      FreeTalkCharacter character) {
    return new FreeTalkSession(learningSessionId, topicId, startMode, character);
  }

  /**
   * 기존 호출부가 기본 캐릭터로 프리톡 세션을 생성한다.
   *
   * @param learningSessionId 연결할 학습 세션 ID
   * @param topicId 선택한 추천 주제 ID
   * @param startMode 첫 발화 주체
   * @return Chloe와 진행 중인 프리톡 세션
   */
  public static FreeTalkSession start(
      Long learningSessionId, Long topicId, FreeTalkStartMode startMode) {
    return start(learningSessionId, topicId, startMode, FreeTalkCharacter.CHLOE);
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

  /** 만료된 외부 AI 처리 표시를 지운다. */
  public boolean clearProcessingIfExpired(LocalDateTime expirationTime) {
    if (processingClientMessageId == null || getUpdatedAt().isAfter(expirationTime)) {
      return false;
    }
    clearProcessing();
    return true;
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
    expressionGenerationStatus = ExpressionGenerationStatus.PREPARING;
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
    expressionGenerationStatus = ExpressionGenerationStatus.PREPARING;
  }

  /**
   * 표현 생성 작업을 실행 중으로 선점한다.
   *
   * @throws IllegalStateException 완료된 프리톡의 준비 상태가 아닐 때
   */
  public void startExpressionGeneration() {
    if (conversationStatus != FreeTalkConversationStatus.COMPLETED
        || expressionGenerationStatus != ExpressionGenerationStatus.PREPARING
        || expressionGenerationStartedAt != null) {
      throw new IllegalStateException("표현 생성 작업을 시작할 수 없는 상태입니다.");
    }
    expressionGenerationStartedAt = LocalDateTime.now();
  }

  /** 표현 생성 결과가 준비됐음을 기록하고 실행 시작 시각을 비운다. */
  public void completeExpressionGeneration() {
    expressionGenerationStatus = ExpressionGenerationStatus.READY;
    expressionGenerationStartedAt = null;
  }

  /** 표현 생성 실패 상태를 기록하고 실행 시작 시각을 비운다. */
  public void failExpressionGeneration() {
    expressionGenerationStatus = ExpressionGenerationStatus.FAILED;
    expressionGenerationStartedAt = null;
  }

  /**
   * 실패한 표현 생성 작업을 다시 준비 상태로 전환한다.
   *
   * @throws IllegalStateException 완료된 프리톡의 실패 상태가 아닐 때
   */
  public void retryExpressionGeneration() {
    if (conversationStatus != FreeTalkConversationStatus.COMPLETED
        || expressionGenerationStatus != ExpressionGenerationStatus.FAILED) {
      throw new IllegalStateException("표현 생성 재시도를 할 수 없는 상태입니다.");
    }
    expressionGenerationStatus = ExpressionGenerationStatus.PREPARING;
  }

  /** 완료된 세션의 장기기억 생성을 등록한다. */
  public void prepareMemoryGeneration() {
    if (conversationStatus == FreeTalkConversationStatus.COMPLETED
        && memoryGenerationStatus == null) {
      memoryGenerationStatus = MemoryGenerationStatus.PREPARING;
    }
  }

  /**
   * 지정한 시각으로 준비된 장기기억 생성 작업을 실행 중으로 선점한다.
   *
   * @param startedAt 작업을 선점한 시각
   * @throws IllegalStateException 완료된 프리톡의 준비 상태가 아니거나 이미 시작된 작업일 때
   */
  public void startMemoryGeneration(LocalDateTime startedAt) {
    if (conversationStatus != FreeTalkConversationStatus.COMPLETED
        || memoryGenerationStatus != MemoryGenerationStatus.PREPARING
        || memoryGenerationStartedAt != null
        || startedAt == null) {
      throw new IllegalStateException("장기기억 생성 작업을 시작할 수 없는 상태입니다.");
    }
    memoryGenerationStartedAt = startedAt;
  }

  /**
   * 실행 중인 장기기억 생성 작업을 완료 상태로 전환한다.
   *
   * @throws IllegalStateException 실행 중인 준비 작업이 아닐 때
   */
  public void completeMemoryGeneration() {
    requireStartedMemoryGeneration();
    memoryGenerationStatus = MemoryGenerationStatus.READY;
    memoryGenerationStartedAt = null;
  }

  /**
   * 준비된 장기기억 생성 작업을 실패 상태로 전환한다.
   *
   * @throws IllegalStateException 완료된 프리톡의 준비 상태가 아닐 때
   */
  public void failMemoryGeneration() {
    if (conversationStatus != FreeTalkConversationStatus.COMPLETED
        || memoryGenerationStatus != MemoryGenerationStatus.PREPARING) {
      throw new IllegalStateException("준비된 장기기억 생성 작업이 아닙니다.");
    }
    memoryGenerationStatus = MemoryGenerationStatus.FAILED;
    memoryGenerationStartedAt = null;
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

  private void requireStartedMemoryGeneration() {
    if (conversationStatus != FreeTalkConversationStatus.COMPLETED
        || memoryGenerationStatus != MemoryGenerationStatus.PREPARING
        || memoryGenerationStartedAt == null) {
      throw new IllegalStateException("실행 중인 장기기억 생성 작업이 아닙니다.");
    }
  }
}
