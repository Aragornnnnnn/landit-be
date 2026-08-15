// 프리톡 세션 시작 결과와 첫 AI 메시지 정보를 제공한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.content.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.domain.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프리톡 세션 시작 결과와 첫 AI 메시지 정보를 제공한다.
 *
 * @param sessionId 생성된 학습 세션 ID
 * @param sessionType 세션 타입
 * @param startMode 첫 발화 주체
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param title 대화 제목
 * @param speakingTimeLimitMs 일일 사용자 발화 시간 제한 밀리초
 * @param ttsVoice AI 상대의 TTS 음성
 * @param currentMessage AI 선시작의 첫 메시지
 */
@Schema(description = "프리톡 세션 시작 응답")
public record FreeTalkSessionStartResponse(
    @Schema(description = "생성된 학습 세션 ID") Long sessionId,
    @Schema(description = "세션 타입", example = "FREE_TALK") String sessionType,
    @Schema(description = "첫 발화 주체") String startMode,
    @Schema(description = "프리톡 캐릭터 식별자", example = "chloe") String characterId,
    @Schema(description = "AI 선시작 주제명. 사용자 선시작은 null") String title,
    @Schema(description = "사용자 일일 발화 시간 제한 밀리초", example = "60000") long speakingTimeLimitMs,
    @Schema(description = "프리톡 AI 상대의 TTS 음성") TtsVoiceResponse ttsVoice,
    @Schema(description = "AI 선시작의 첫 AI 메시지. 사용자 선시작은 null")
        CurrentMessageResponse currentMessage) {

  private static final long SPEAKING_TIME_LIMIT_MS = 60_000L;

  /**
   * 생성된 프리톡 세션을 공개 응답으로 변환한다.
   *
   * @param sessionId 생성된 학습 세션 ID
   * @param startMode 첫 발화 주체
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param title 대화 제목
   * @param ttsVoice AI 상대의 TTS 음성
   * @param currentMessage AI 선시작의 첫 메시지
   * @return 프리톡 세션 시작 응답
   */
  public static FreeTalkSessionStartResponse from(
      Long sessionId,
      String startMode,
      String characterId,
      String title,
      TtsVoiceResponse ttsVoice,
      CurrentMessageResponse currentMessage) {
    return new FreeTalkSessionStartResponse(
        sessionId,
        SessionType.FREE_TALK.name(),
        startMode,
        characterId,
        title,
        SPEAKING_TIME_LIMIT_MS,
        ttsVoice,
        currentMessage);
  }

  /**
   * 프리톡 현재 AI 메시지 응답이다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 대화 턴 번호
   * @param messageSequence 세션 내 메시지 순서
   * @param role 메시지 화자 역할
   * @param content AI 메시지 원문
   * @param translatedContent AI 메시지 번역문
   * @param emotion AI 캐릭터 감정
   */
  @Schema(name = "FreeTalkCurrentMessageResponse", description = "프리톡 현재 AI 메시지")
  public record CurrentMessageResponse(
      @Schema(description = "메시지 ID") Long messageId,
      @Schema(description = "대화 턴 번호") int turnNumber,
      @Schema(description = "세션 내 메시지 순서") int messageSequence,
      @Schema(description = "발화 주체", example = "AI") String role,
      @Schema(description = "AI 메시지 원문") String content,
      @Schema(description = "AI 메시지 기준 언어 번역") String translatedContent,
      @Schema(description = "AI 캐릭터 감정") String emotion) {

    /**
     * 저장된 프리톡 AI 메시지를 응답으로 변환한다.
     *
     * @param message 변환할 AI 메시지
     * @return 현재 AI 메시지 응답
     */
    public static CurrentMessageResponse from(SessionHistoryMessage message) {
      return new CurrentMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          message.getContent(),
          message.getTranslatedContent(),
          message.getEmotion() == null ? null : message.getEmotion().name());
    }
  }
}
