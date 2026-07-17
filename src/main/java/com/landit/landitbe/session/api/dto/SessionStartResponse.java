// 시나리오 세션 시작 API의 응답 구조를 정의한다.

package com.landit.landitbe.session.api.dto;

import com.landit.landitbe.content.api.dto.TtsVoiceResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 시나리오 세션 시작 API의 응답 구조를 정의한다. */
@Schema(description = "시나리오 세션 시작 응답")
public record SessionStartResponse(
    @Schema(description = "생성된 학습 세션 ID") Long sessionId,
    @Schema(description = "시나리오 ID") Long scenarioId,
    @Schema(description = "세션 타입") String sessionType,
    @Schema(description = "첫 발화자") String firstSpeaker,
    @Schema(description = "USER first 시 사용자 시작 안내") String userOpeningInstruction,
    @Schema(description = "활성 시나리오 TTS 음성. 미설정 또는 비활성 음성이면 null") TtsVoiceResponse ttsVoice,
    @Schema(description = "AI first 시 생성된 현재 메시지") CurrentMessageResponse currentMessage,
    @Schema(description = "세션 진행도") SessionProgressResponse progress) {

  /** 내부 타입을 정의한다. */
  @Schema(description = "현재 메시지 응답")
  public record CurrentMessageResponse(
      @Schema(description = "메시지 ID") Long messageId,
      @Schema(description = "턴 번호") int turnNumber,
      @Schema(description = "세션 히스토리 안 메시지 순서") int messageSequence,
      @Schema(description = "발화 주체") String role,
      @Schema(description = "메시지 본문") String content,
      @Schema(description = "기준 locale 번역") String translatedContent,
      @Schema(description = "첫 화면에 보여줄 상대 역할의 속마음") String innerThought,
      @Schema(description = "속마음 유형") String innerThoughtType) {}

  /** 내부 타입을 정의한다. */
  @Schema(description = "세션 진행도 응답")
  public record SessionProgressResponse(
      @Schema(description = "현재 턴 번호") int currentTurnNumber,
      @Schema(description = "고정 질문 개수") int totalQuestionCount,
      @Schema(description = "세션 완료 여부") boolean completed) {}
}
