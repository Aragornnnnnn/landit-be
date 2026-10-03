// 업무 간에 전달할 StartedFreeTalkSession 값을 정의한다.

package com.landit.landitbe.feature.learning.freetalk.start.dto;

import com.landit.landitbe.feature.content.tutor.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.AiFreeTalkPendingFollowUp;

/**
 * 외부 AI 호출과 응답 생성에 필요한 시작 레코드 정보다.
 *
 * @param learningSessionId 생성된 학습 세션 ID
 * @param sessionHistoryId 생성된 세션 히스토리 ID
 * @param freeTalkSessionId 생성된 프리톡 세션 ID
 * @param startMode 첫 발화 주체
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param topicId 선택한 주제 ID
 * @param title 대화 제목
 * @param topicPromptDescription AI에 전달할 주제 설명
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 사용자 기준 언어
 * @param ttsVoice AI 상대의 TTS 음성
 * @param pendingFollowUp 선택한 예고 질문. 일반 시작이면 null
 */
public record StartedFreeTalkSession(
    Long learningSessionId,
    Long sessionHistoryId,
    Long freeTalkSessionId,
    FreeTalkStartMode startMode,
    String characterId,
    Long topicId,
    String title,
    String topicPromptDescription,
    String targetLocale,
    String baseLocale,
    TtsVoiceResponse ttsVoice,
    AiFreeTalkPendingFollowUp pendingFollowUp) {

  /**
   * 기존 일반 시작 세션 값과 호환되는 생성자다.
   *
   * @param learningSessionId 학습 세션 ID
   * @param sessionHistoryId 세션 기록 ID
   * @param freeTalkSessionId 프리톡 세션 ID
   * @param startMode 시작 방식
   * @param characterId 캐릭터 ID
   * @param topicId 추천 주제 ID
   * @param title 제목
   * @param topicPromptDescription 주제 설명
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param ttsVoice TTS 음성
   */
  public StartedFreeTalkSession(
      Long learningSessionId,
      Long sessionHistoryId,
      Long freeTalkSessionId,
      FreeTalkStartMode startMode,
      String characterId,
      Long topicId,
      String title,
      String topicPromptDescription,
      String targetLocale,
      String baseLocale,
      TtsVoiceResponse ttsVoice) {
    this(
        learningSessionId,
        sessionHistoryId,
        freeTalkSessionId,
        startMode,
        characterId,
        topicId,
        title,
        topicPromptDescription,
        targetLocale,
        baseLocale,
        ttsVoice,
        null);
  }
}
