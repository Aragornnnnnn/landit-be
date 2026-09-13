// 업무 간에 전달할 StartedFreeTalkSession 값을 정의한다.

package com.landit.landitbe.feature.session.freetalk.dto;

import com.landit.landitbe.feature.content.tutor.dto.TtsVoiceResponse;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkStartMode;

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
    TtsVoiceResponse ttsVoice) {}
