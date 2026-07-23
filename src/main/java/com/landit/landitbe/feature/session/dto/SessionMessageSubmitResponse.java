// 사용자 발화 제출 API의 응답 구조를 정의한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자 발화 제출 API의 응답 구조를 정의한다.
 *
 * @param sessionId 세션 ID
 * @param submittedMessage 제출된 사용자 메시지
 * @param nextMessage 다음 AI 메시지
 * @param progress 세션 진행도
 */
@Schema(description = "사용자 발화 제출 응답")
public record SessionMessageSubmitResponse(
    @Schema(description = "세션 ID") Long sessionId,
    @Schema(description = "제출된 사용자 메시지") SubmittedMessageResponse submittedMessage,
    @Schema(description = "다음 AI 메시지") NextMessageResponse nextMessage,
    @Schema(description = "세션 진행도") SessionProgressResponse progress) {

  /** 저장된 사용자·AI 메시지와 진행 상태를 제출 응답으로 변환한다. */
  public static SessionMessageSubmitResponse from(
      Long sessionId,
      SessionHistoryMessage submittedMessage,
      ProcessingStatus feedbackProcessingStatus,
      SessionHistoryMessage nextMessage,
      int totalQuestionCount,
      boolean completed) {
    return new SessionMessageSubmitResponse(
        sessionId,
        SubmittedMessageResponse.from(submittedMessage, feedbackProcessingStatus),
        NextMessageResponse.from(nextMessage),
        SessionProgressResponse.from(nextMessage, totalQuestionCount, completed));
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 턴 번호
   * @param messageSequence 세션 히스토리 안 메시지 순서
   * @param role 발화 주체
   * @param feedbackProcessingStatus 메시지별 피드백 처리 상태. 정상 접수 시 PREPARING
   * @param innerThoughtProcessingStatus 상대 역할 속마음 처리 상태
   * @param innerThought 상대 역할의 속마음
   * @param innerThoughtType 속마음 유형
   */
  @Schema(description = "제출된 사용자 메시지 응답")
  public record SubmittedMessageResponse(
      @Schema(description = "메시지 ID") Long messageId,
      @Schema(description = "턴 번호") int turnNumber,
      @Schema(description = "세션 히스토리 안 메시지 순서") int messageSequence,
      @Schema(description = "발화 주체") String role,
      @Schema(
              description = "메시지별 피드백 처리 상태. 정상 접수 시 PREPARING",
              allowableValues = {"PREPARING", "COMPLETED", "FAILED"})
          String feedbackProcessingStatus,
      @Schema(
              description = "상대 역할 속마음 처리 상태",
              allowableValues = {"PREPARING", "COMPLETED", "FAILED"})
          String innerThoughtProcessingStatus,
      @Schema(description = "상대 역할의 속마음") String innerThought,
      @Schema(description = "속마음 유형") String innerThoughtType) {

    /** 저장된 사용자 메시지와 피드백 상태를 응답으로 변환한다. */
    public static SubmittedMessageResponse from(
        SessionHistoryMessage message, ProcessingStatus feedbackProcessingStatus) {
      return new SubmittedMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          feedbackProcessingStatus.name(),
          message.getInnerThoughtProcessingStatus().name(),
          message.getInnerThought(),
          message.getInnerThoughtType() == null ? null : message.getInnerThoughtType().name());
    }
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 턴 번호
   * @param messageSequence 세션 히스토리 안 메시지 순서
   * @param role 발화 주체
   * @param content 메시지 본문
   * @param translatedContent 기준 locale 번역
   */
  @Schema(description = "다음 AI 메시지 응답")
  public record NextMessageResponse(
      @Schema(description = "메시지 ID") Long messageId,
      @Schema(description = "턴 번호") int turnNumber,
      @Schema(description = "세션 히스토리 안 메시지 순서") int messageSequence,
      @Schema(description = "발화 주체") String role,
      @Schema(description = "메시지 본문") String content,
      @Schema(description = "기준 locale 번역") String translatedContent) {

    /** 저장된 AI 메시지를 다음 메시지 응답으로 변환한다. */
    public static NextMessageResponse from(SessionHistoryMessage message) {
      return new NextMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          message.getContent(),
          message.getTranslatedContent());
    }
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param currentTurnNumber 현재 턴 번호
   * @param currentMessageSequenceNumber 현재 턴의 메시지 순서
   * @param totalQuestionCount 고정 질문 개수
   * @param completed 세션 완료 여부
   */
  @Schema(description = "세션 진행도 응답")
  public record SessionProgressResponse(
      @Schema(description = "현재 턴 번호") int currentTurnNumber,
      @Schema(description = "현재 턴의 메시지 순서") int currentMessageSequenceNumber,
      @Schema(description = "고정 질문 개수") int totalQuestionCount,
      @Schema(description = "세션 완료 여부") boolean completed) {

    /** 다음 AI 메시지와 콘텐츠 질문 수를 세션 진행도 응답으로 변환한다. */
    public static SessionProgressResponse from(
        SessionHistoryMessage nextMessage, int totalQuestionCount, boolean completed) {
      return new SessionProgressResponse(
          nextMessage.getTurnNumber(), 2, totalQuestionCount, completed);
    }
  }
}
