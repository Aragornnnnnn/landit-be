// 프리톡 세션 시작 결과와 첫 AI 메시지 정보를 제공한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.content.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.domain.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;

/** 프리톡 세션 시작 결과와 첫 AI 메시지 정보를 제공한다. */
@Schema(description = "프리톡 세션 시작 응답")
public record FreeTalkSessionStartResponse(
    @Schema(description = "생성된 학습 세션 ID") Long sessionId,
    @Schema(description = "세션 타입", example = "FREE_TALK") String sessionType,
    @Schema(description = "첫 발화 주체") String startMode,
    @Schema(description = "AI 선시작 주제명. 사용자 선시작은 null") String title,
    @Schema(description = "사용자 누적 발화 시간 제한 밀리초", example = "180000") long speakingTimeLimitMs,
    @Schema(description = "프리톡 AI 상대의 TTS 음성") TtsVoiceResponse ttsVoice,
    @Schema(description = "AI 선시작의 첫 AI 메시지. 사용자 선시작은 null")
        CurrentMessageResponse currentMessage) {

  private static final long SPEAKING_TIME_LIMIT_MS = 180_000L;

  /** 생성된 프리톡 세션을 공개 응답으로 변환한다. */
  public static FreeTalkSessionStartResponse from(
      Long sessionId,
      String startMode,
      String title,
      TtsVoiceResponse ttsVoice,
      CurrentMessageResponse currentMessage) {
    return new FreeTalkSessionStartResponse(
        sessionId,
        SessionType.FREE_TALK.name(),
        startMode,
        title,
        SPEAKING_TIME_LIMIT_MS,
        ttsVoice,
        currentMessage);
  }

  /** 첫 AI 메시지를 저장 결과에서 공개 응답으로 변환한다. */
  @Schema(description = "프리톡 현재 AI 메시지")
  public record CurrentMessageResponse(
      @Schema(description = "메시지 ID") Long messageId,
      @Schema(description = "대화 턴 번호") int turnNumber,
      @Schema(description = "세션 내 메시지 순서") int messageSequence,
      @Schema(description = "발화 주체", example = "AI") String role,
      @Schema(description = "AI 메시지 원문") String content,
      @Schema(description = "AI 메시지 기준 언어 번역") String translatedContent,
      @Schema(description = "AI 캐릭터 감정") String emotion) {

    /** 저장된 프리톡 AI 메시지를 응답으로 변환한다. */
    public static CurrentMessageResponse from(SessionHistoryMessage message) {
      return new CurrentMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          message.getContent(),
          message.getTranslatedContent(),
          message.getEmotion().name());
    }
  }
}
