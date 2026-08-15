// 프리톡 발화 처리 결과와 진행 상태를 반환한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프리톡 발화 처리 결과와 진행 상태를 반환한다.
 *
 * @param sessionId 프리톡 학습 세션 ID
 * @param title 대화에서 추론하거나 선택한 세션 제목
 * @param turnStatus 이번 발화 처리 결과 상태
 * @param submittedMessage 저장된 사용자 발화 정보
 * @param nextMessage 계속 대화할 때 생성된 AI 후속 메시지
 * @param progress 누적 발화 시간과 세션 진행 상태
 */
public record FreeTalkMessageSubmitResponse(
    Long sessionId,
    String title,
    FreeTalkTurnStatus turnStatus,
    SubmittedMessageResponse submittedMessage,
    NextMessageResponse nextMessage,
    ProgressResponse progress) {

  /**
   * 사용자가 제출한 발화의 응답 표현이다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 대화 턴 번호
   * @param messageSequence 세션 내 메시지 순서
   * @param role 메시지 화자 역할
   * @param innerThought AI가 생성한 속마음
   * @param innerThoughtType 속마음 상태 유형
   * @param innerThoughtProcessingStatus 속마음 비동기 처리 상태
   */
  @Schema(name = "FreeTalkSubmittedMessageResponse", description = "프리톡 사용자 제출 메시지")
  public record SubmittedMessageResponse(
      Long messageId,
      int turnNumber,
      int messageSequence,
      String role,
      String innerThought,
      InnerThoughtType innerThoughtType,
      ProcessingStatus innerThoughtProcessingStatus) {
    /**
     * 세션 메시지 엔티티를 응답 값으로 변환한다.
     *
     * @param message 변환할 사용자 메시지
     * @return 사용자 발화 응답
     */
    public static SubmittedMessageResponse from(SessionHistoryMessage message) {
      return new SubmittedMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          message.getInnerThought(),
          message.getInnerThoughtType(),
          message.getInnerThoughtProcessingStatus());
    }

    /**
     * 최초 응답과 같은 비동기 속마음 준비 상태를 다시 구성한다.
     *
     * @param message 준비 상태로 재구성할 사용자 메시지
     * @return 속마음 처리가 준비 중인 사용자 발화 응답
     */
    public static SubmittedMessageResponse replayPreparingFrom(SessionHistoryMessage message) {
      return new SubmittedMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          null,
          null,
          ProcessingStatus.PREPARING);
    }
  }

  /**
   * AI 후속 발화의 응답 표현이다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 대화 턴 번호
   * @param messageSequence 세션 내 메시지 순서
   * @param role 메시지 화자 역할
   * @param content AI 발화 원문
   * @param translatedContent AI 발화 번역문
   * @param emotion 캐릭터 감정 상태
   */
  @Schema(name = "FreeTalkNextMessageResponse", description = "프리톡 AI 후속 메시지")
  public record NextMessageResponse(
      Long messageId,
      int turnNumber,
      int messageSequence,
      String role,
      String content,
      String translatedContent,
      CharacterEmotion emotion) {
    /**
     * 세션 메시지 엔티티를 응답 값으로 변환한다.
     *
     * @param message 변환할 AI 메시지
     * @return AI 후속 메시지 응답
     */
    public static NextMessageResponse from(SessionHistoryMessage message) {
      return new NextMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          message.getContent(),
          message.getTranslatedContent(),
          message.getEmotion());
    }
  }

  /**
   * 프리톡 진행 상태의 응답 표현이다.
   *
   * @param sessionStatus 현재 프리톡 대화 상태
   * @param accumulatedSpeakingDurationMs 현재 세션의 누적 사용자 발화 시간 밀리초
   * @param speakingTimeLimitMs 일일 사용자 발화 시간 제한 밀리초
   * @param usedSpeakingTimeMs KST 당일 사용한 사용자 발화 시간 밀리초
   * @param remainingSpeakingTimeMs KST 당일 남은 사용자 발화 시간 밀리초
   * @param expressionGenerationStatus 맞춤 표현 생성 상태
   */
  public record ProgressResponse(
      FreeTalkConversationStatus sessionStatus,
      long accumulatedSpeakingDurationMs,
      long speakingTimeLimitMs,
      long usedSpeakingTimeMs,
      long remainingSpeakingTimeMs,
      ExpressionGenerationStatus expressionGenerationStatus) {}
}
