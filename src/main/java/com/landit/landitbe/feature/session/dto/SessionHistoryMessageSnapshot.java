// 영속 SessionHistoryMessage의 조회 상태를 분리된 값으로 전달한다.

package com.landit.landitbe.feature.session.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.history.domain.SessionHistoryMessage;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;

/**
 * 대화 저장소와 분리된 조회 시점의 상태다.
 *
 * @param id 식별자
 * @param sessionHistoryId 대화 이력 ID
 * @param messageSequence 메시지 순서
 * @param turnNumber 대화 턴
 * @param role 발화 주체
 * @param content 발화 본문
 * @param translatedContent 번역 본문
 * @param clientMessageId 재전송 식별자
 * @param scenarioAttemptToken 시나리오 생성 시도 식별자
 * @param scenarioLeaseUntil 시나리오 생성 선점 만료
 * @param scenarioResponsePayload 시나리오 재전송 응답
 * @param utteranceDurationMs 발화 시간 밀리초
 * @param freeTalkTurnStatus 프리톡 턴 결과
 * @param emotion 캐릭터 감정
 * @param inputType 입력 방식
 * @param innerThought 속마음
 * @param innerThoughtType 속마음 종류
 * @param innerThoughtProcessingStatus 속마음 생성 상태
 * @param feedbackProcessingStatus 피드백 생성 상태
 * @param pronunciationScore 발음 점수
 * @param intonationScore 억양 점수
 * @param fluencyScore 유창성 점수
 * @param speechAnalysisPayload 발화 분석 값
 * @param reusedExpressionPayload 재사용 표현 값
 * @param createdAt 생성 시각
 * @param updatedAt 수정 시각
 */
public record SessionHistoryMessageSnapshot(
    Long id,
    Long sessionHistoryId,
    int messageSequence,
    int turnNumber,
    ConversationSpeaker role,
    String content,
    String translatedContent,
    String clientMessageId,
    String scenarioAttemptToken,
    java.time.LocalDateTime scenarioLeaseUntil,
    String scenarioResponsePayload,
    Long utteranceDurationMs,
    FreeTalkTurnStatus freeTalkTurnStatus,
    CharacterEmotion emotion,
    SessionMessageInputType inputType,
    String innerThought,
    InnerThoughtType innerThoughtType,
    ProcessingStatus innerThoughtProcessingStatus,
    ProcessingStatus feedbackProcessingStatus,
    Integer pronunciationScore,
    Integer intonationScore,
    Integer fluencyScore,
    JsonNode speechAnalysisPayload,
    JsonNode reusedExpressionPayload,
    java.time.LocalDateTime createdAt,
    java.time.LocalDateTime updatedAt) {
  /**
   * 영속 상태를 복사한다.
   *
   * @param entity 원본 상태
   * @return 조회 시점의 값
   */
  public static SessionHistoryMessageSnapshot from(SessionHistoryMessage entity) {
    return new SessionHistoryMessageSnapshot(
        entity.getId(),
        entity.getSessionHistoryId(),
        entity.getMessageSequence(),
        entity.getTurnNumber(),
        entity.getRole(),
        entity.getContent(),
        entity.getTranslatedContent(),
        entity.getClientMessageId(),
        entity.getScenarioAttemptToken(),
        entity.getScenarioLeaseUntil(),
        entity.getScenarioResponsePayload(),
        entity.getUtteranceDurationMs(),
        entity.getFreeTalkTurnStatus(),
        entity.getEmotion(),
        entity.getInputType(),
        entity.getInnerThought(),
        entity.getInnerThoughtType(),
        entity.getInnerThoughtProcessingStatus(),
        entity.getFeedbackProcessingStatus(),
        entity.getPronunciationScore(),
        entity.getIntonationScore(),
        entity.getFluencyScore(),
        entity.getSpeechAnalysisPayload() == null
            ? null
            : entity.getSpeechAnalysisPayload().deepCopy(),
        entity.getReusedExpressionPayload() == null
            ? null
            : entity.getReusedExpressionPayload().deepCopy(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  /**
   * 식별자 값을 반환한다.
   *
   * @return 조회 당시 식별자
   */
  public Long getId() {
    return id;
  }

  /**
   * 대화 이력 ID 값을 반환한다.
   *
   * @return 조회 당시 대화 이력 ID
   */
  public Long getSessionHistoryId() {
    return sessionHistoryId;
  }

  /**
   * 메시지 순서 값을 반환한다.
   *
   * @return 조회 당시 메시지 순서
   */
  public int getMessageSequence() {
    return messageSequence;
  }

  /**
   * 대화 턴 값을 반환한다.
   *
   * @return 조회 당시 대화 턴
   */
  public int getTurnNumber() {
    return turnNumber;
  }

  /**
   * 발화 주체 값을 반환한다.
   *
   * @return 조회 당시 발화 주체
   */
  public ConversationSpeaker getRole() {
    return role;
  }

  /**
   * 발화 본문 값을 반환한다.
   *
   * @return 조회 당시 발화 본문
   */
  public String getContent() {
    return content;
  }

  /**
   * 번역 본문 값을 반환한다.
   *
   * @return 조회 당시 번역 본문
   */
  public String getTranslatedContent() {
    return translatedContent;
  }

  /**
   * 재전송 식별자 값을 반환한다.
   *
   * @return 조회 당시 재전송 식별자
   */
  public String getClientMessageId() {
    return clientMessageId;
  }

  /**
   * 시나리오 생성 시도 식별자 값을 반환한다.
   *
   * @return 조회 당시 시나리오 생성 시도 식별자
   */
  public String getScenarioAttemptToken() {
    return scenarioAttemptToken;
  }

  /**
   * 시나리오 생성 선점 만료 값을 반환한다.
   *
   * @return 조회 당시 시나리오 생성 선점 만료
   */
  public java.time.LocalDateTime getScenarioLeaseUntil() {
    return scenarioLeaseUntil;
  }

  /**
   * 시나리오 재전송 응답 값을 반환한다.
   *
   * @return 조회 당시 시나리오 재전송 응답
   */
  public String getScenarioResponsePayload() {
    return scenarioResponsePayload;
  }

  /**
   * 발화 시간 밀리초 값을 반환한다.
   *
   * @return 조회 당시 발화 시간 밀리초
   */
  public Long getUtteranceDurationMs() {
    return utteranceDurationMs;
  }

  /**
   * 프리톡 턴 결과 값을 반환한다.
   *
   * @return 조회 당시 프리톡 턴 결과
   */
  public FreeTalkTurnStatus getFreeTalkTurnStatus() {
    return freeTalkTurnStatus;
  }

  /**
   * 캐릭터 감정 값을 반환한다.
   *
   * @return 조회 당시 캐릭터 감정
   */
  public CharacterEmotion getEmotion() {
    return emotion;
  }

  /**
   * 입력 방식 값을 반환한다.
   *
   * @return 조회 당시 입력 방식
   */
  public SessionMessageInputType getInputType() {
    return inputType;
  }

  /**
   * 속마음 값을 반환한다.
   *
   * @return 조회 당시 속마음
   */
  public String getInnerThought() {
    return innerThought;
  }

  /**
   * 속마음 종류 값을 반환한다.
   *
   * @return 조회 당시 속마음 종류
   */
  public InnerThoughtType getInnerThoughtType() {
    return innerThoughtType;
  }

  /**
   * 속마음 생성 상태 값을 반환한다.
   *
   * @return 조회 당시 속마음 생성 상태
   */
  public ProcessingStatus getInnerThoughtProcessingStatus() {
    return innerThoughtProcessingStatus;
  }

  /**
   * 피드백 생성 상태 값을 반환한다.
   *
   * @return 조회 당시 피드백 생성 상태
   */
  public ProcessingStatus getFeedbackProcessingStatus() {
    return feedbackProcessingStatus;
  }

  /**
   * 발음 점수 값을 반환한다.
   *
   * @return 조회 당시 발음 점수
   */
  public Integer getPronunciationScore() {
    return pronunciationScore;
  }

  /**
   * 억양 점수 값을 반환한다.
   *
   * @return 조회 당시 억양 점수
   */
  public Integer getIntonationScore() {
    return intonationScore;
  }

  /**
   * 유창성 점수 값을 반환한다.
   *
   * @return 조회 당시 유창성 점수
   */
  public Integer getFluencyScore() {
    return fluencyScore;
  }

  /**
   * 발화 분석 값 값을 반환한다.
   *
   * @return 조회 당시 발화 분석 값
   */
  public JsonNode getSpeechAnalysisPayload() {
    return speechAnalysisPayload;
  }

  /**
   * 재사용 표현 값 값을 반환한다.
   *
   * @return 조회 당시 재사용 표현 값
   */
  public JsonNode getReusedExpressionPayload() {
    return reusedExpressionPayload;
  }

  /**
   * 생성 시각 값을 반환한다.
   *
   * @return 조회 당시 생성 시각
   */
  public java.time.LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * 수정 시각 값을 반환한다.
   *
   * @return 조회 당시 수정 시각
   */
  public java.time.LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
