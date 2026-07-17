// 사용자 발화 제출 API의 응답 구조를 정의한다.

package com.landit.landitbe.session.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 사용자 발화 제출 API의 응답 구조를 정의한다. */
@Schema(description = "사용자 발화 제출 응답")
public record SessionMessageSubmitResponse(
    @Schema(description = "세션 ID") Long sessionId,
    @Schema(description = "제출된 사용자 메시지") SubmittedMessageResponse submittedMessage,
    @Schema(description = "다음 AI 메시지") NextMessageResponse nextMessage,
    @Schema(description = "세션 진행도") SessionProgressResponse progress) {

  /** 내부 타입을 정의한다. */
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
      @Schema(description = "속마음 유형") String innerThoughtType) {}

  /** 내부 타입을 정의한다. */
  @Schema(description = "다음 AI 메시지 응답")
  public record NextMessageResponse(
      @Schema(description = "메시지 ID") Long messageId,
      @Schema(description = "턴 번호") int turnNumber,
      @Schema(description = "세션 히스토리 안 메시지 순서") int messageSequence,
      @Schema(description = "발화 주체") String role,
      @Schema(description = "메시지 본문") String content,
      @Schema(description = "기준 locale 번역") String translatedContent) {}

  /** 내부 타입을 정의한다. */
  @Schema(description = "세션 진행도 응답")
  public record SessionProgressResponse(
      @Schema(description = "현재 턴 번호") int currentTurnNumber,
      @Schema(description = "현재 턴의 메시지 순서") int currentMessageSequenceNumber,
      @Schema(description = "고정 질문 개수") int totalQuestionCount,
      @Schema(description = "세션 완료 여부") boolean completed) {}
}
