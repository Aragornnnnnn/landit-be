// 세션 히스토리에 남길 AI와 사용자 메시지를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 세션 히스토리에 남길 AI와 사용자 메시지를 저장한다. */
@Getter
@Entity
@Table(name = "session_history_message")
public class SessionHistoryMessage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_history_id", nullable = false)
  private Long sessionHistoryId;

  @Column(name = "message_sequence", nullable = false)
  private int messageSequence;

  @Column(name = "turn_number", nullable = false)
  private int turnNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ConversationSpeaker role;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "translated_content", columnDefinition = "text")
  private String translatedContent;

  @Column(name = "client_message_id", length = 36)
  private String clientMessageId;

  @Column(name = "utterance_duration_ms")
  private Long utteranceDurationMs;

  @Enumerated(EnumType.STRING)
  @Column(name = "free_talk_turn_status", length = 40)
  private FreeTalkTurnStatus freeTalkTurnStatus;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CharacterEmotion emotion;

  @Enumerated(EnumType.STRING)
  @Column(name = "input_type", nullable = false, length = 20)
  private SessionMessageInputType inputType;

  @Column(name = "inner_thought", columnDefinition = "text")
  private String innerThought;

  @Enumerated(EnumType.STRING)
  @Column(name = "inner_thought_type", length = 20)
  private InnerThoughtType innerThoughtType;

  @Enumerated(EnumType.STRING)
  @Column(name = "inner_thought_processing_status", length = 20)
  private ProcessingStatus innerThoughtProcessingStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "feedback_processing_status", length = 20)
  private ProcessingStatus feedbackProcessingStatus;

  @Column(name = "pronunciation_score")
  private Integer pronunciationScore;

  @Column(name = "intonation_score")
  private Integer intonationScore;

  @Column(name = "fluency_score")
  private Integer fluencyScore;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "speech_analysis_payload", columnDefinition = "jsonb")
  private JsonNode speechAnalysisPayload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reused_expression_payload", columnDefinition = "jsonb")
  private JsonNode reusedExpressionPayload;

  /** JPA에서 사용하는 기본 생성자다. */
  protected SessionHistoryMessage() {}

  private SessionHistoryMessage(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      ConversationSpeaker role,
      String content,
      String translatedContent,
      SessionMessageInputType inputType,
      String innerThought,
      InnerThoughtType innerThoughtType) {
    this.sessionHistoryId = sessionHistoryId;
    this.messageSequence = messageSequence;
    this.turnNumber = turnNumber;
    this.role = role;
    this.content = content;
    this.translatedContent = translatedContent;
    this.inputType = inputType;
    this.innerThought = innerThought;
    this.innerThoughtType = innerThoughtType;
  }

  /** AI first 시나리오의 첫 AI 메시지를 생성한다. */
  public static SessionHistoryMessage aiOpening(
      Long sessionHistoryId,
      String content,
      String translatedContent,
      String innerThought,
      InnerThoughtType innerThoughtType) {
    return new SessionHistoryMessage(
        sessionHistoryId,
        1,
        1,
        ConversationSpeaker.AI,
        content,
        translatedContent,
        SessionMessageInputType.GENERATED,
        innerThought,
        innerThoughtType);
  }

  /** 사용자가 제출한 메시지를 생성한다. */
  public static SessionHistoryMessage user(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String content,
      SessionMessageInputType inputType) {
    SessionHistoryMessage message =
        new SessionHistoryMessage(
            sessionHistoryId,
            messageSequence,
            turnNumber,
            ConversationSpeaker.USER,
            content,
            null,
            inputType,
            null,
            null);
    message.innerThoughtProcessingStatus = ProcessingStatus.PREPARING;
    message.feedbackProcessingStatus = ProcessingStatus.PREPARING;
    return message;
  }

  /**
   * 프리톡 사용자의 메시지를 피드백 처리 상태 없이 생성한다.
   *
   * @param sessionHistoryId 연결할 세션 히스토리 ID
   * @param messageSequence 세션 내 메시지 순서
   * @param turnNumber 대화 턴 번호
   * @param clientMessageId 클라이언트 멱등성 메시지 ID
   * @param content 사용자 발화 원문
   * @param inputType 발화 입력 방식
   * @param utteranceDurationMs 사용자 발화 시간 밀리초
   * @return 저장할 프리톡 사용자 메시지
   * @throws IllegalArgumentException 발화 시간이 음수일 때
   */
  public static SessionHistoryMessage freeTalkUser(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String clientMessageId,
      String content,
      SessionMessageInputType inputType,
      long utteranceDurationMs) {
    if (utteranceDurationMs < 0) {
      throw new IllegalArgumentException("사용자 발화 시간은 0 이상이어야 합니다.");
    }
    SessionHistoryMessage message =
        new SessionHistoryMessage(
            sessionHistoryId,
            messageSequence,
            turnNumber,
            ConversationSpeaker.USER,
            content,
            null,
            inputType,
            null,
            null);
    message.clientMessageId = clientMessageId;
    message.utteranceDurationMs = utteranceDurationMs;
    return message;
  }

  /**
   * 프리톡 AI 메시지를 캐릭터 감정과 함께 생성한다.
   *
   * @param sessionHistoryId 연결할 세션 히스토리 ID
   * @param messageSequence 세션 내 메시지 순서
   * @param turnNumber 대화 턴 번호
   * @param content AI 메시지 원문
   * @param translatedContent AI 메시지 번역문
   * @param emotion AI 캐릭터 감정
   * @return 저장할 프리톡 AI 메시지
   */
  public static SessionHistoryMessage freeTalkAi(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String content,
      String translatedContent,
      CharacterEmotion emotion) {
    SessionHistoryMessage message =
        aiGenerated(sessionHistoryId, messageSequence, turnNumber, content, translatedContent);
    message.emotion = emotion;
    return message;
  }

  /** 히스토리에 저장할 AI 후속 메시지를 생성한다. */
  public static SessionHistoryMessage aiGenerated(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String content,
      String translatedContent) {
    return new SessionHistoryMessage(
        sessionHistoryId,
        messageSequence,
        turnNumber,
        ConversationSpeaker.AI,
        content,
        translatedContent,
        SessionMessageInputType.GENERATED,
        null,
        null);
  }

  /** 사용자 메시지를 들은 상대 역할의 속마음을 기록한다. */
  public void recordInnerThought(String innerThought, InnerThoughtType innerThoughtType) {
    this.innerThought = innerThought;
    this.innerThoughtType = innerThoughtType;
    this.innerThoughtProcessingStatus = ProcessingStatus.COMPLETED;
  }

  /** 프리톡 속마음의 비동기 생성을 준비 상태로 표시한다. */
  public void prepareInnerThought() {
    innerThoughtProcessingStatus = ProcessingStatus.PREPARING;
  }

  /** 프리톡 사용자 발화의 최초 처리 결과 상태를 기록한다. */
  public void recordFreeTalkTurnStatus(FreeTalkTurnStatus freeTalkTurnStatus) {
    this.freeTalkTurnStatus = freeTalkTurnStatus;
  }

  /** 생성에 실패한 속마음의 처리 상태를 기록한다. */
  public void markInnerThoughtFailed() {
    if (innerThoughtProcessingStatus == ProcessingStatus.PREPARING) {
      innerThoughtProcessingStatus = ProcessingStatus.FAILED;
    }
  }
}
