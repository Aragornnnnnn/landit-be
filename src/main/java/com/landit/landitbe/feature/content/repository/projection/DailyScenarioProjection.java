// 날짜별 시나리오 조회에 필요한 단건 콘텐츠 정보를 담는다.

package com.landit.landitbe.feature.content.repository.projection;

import com.landit.landitbe.feature.content.domain.ScenarioDifficulty;
import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.math.BigDecimal;

/**
 * 날짜별 시나리오 조회에 필요한 단건 콘텐츠 정보를 담는다.
 *
 * @param scenarioId 시나리오 ID
 * @param characterId 시나리오 캐릭터 식별자
 * @param scenarioTitle 시나리오 제목
 * @param briefing 시나리오 설명
 * @param conversationGoal 대화 목표
 * @param thumbnailUrl 시나리오 썸네일 URL
 * @param difficulty 시나리오 난이도
 * @param firstSpeaker 첫 발화자
 * @param aiOpeningMessage AI 첫 메시지
 * @param aiOpeningMessageTranslation AI 첫 메시지 번역
 * @param openingQuestionAudioUrl 첫 고정 질문 음원 URL
 * @param userOpeningInstruction 사용자 첫 발화 안내
 * @param innerThought AI 속마음
 * @param innerThoughtType AI 속마음 유형
 * @param ttsVoiceProvider TTS Provider
 * @param ttsVoiceModel TTS 모델
 * @param providerVoiceId TTS 제공자 음성 ID
 * @param ttsVoiceGender TTS 음성 성별
 * @param bestStarRating 사용자 최고 별점
 */
public record DailyScenarioProjection(
    Long scenarioId,
    String characterId,
    String scenarioTitle,
    String briefing,
    String conversationGoal,
    String thumbnailUrl,
    ScenarioDifficulty difficulty,
    ConversationSpeaker firstSpeaker,
    String aiOpeningMessage,
    String aiOpeningMessageTranslation,
    String openingQuestionAudioUrl,
    String userOpeningInstruction,
    String innerThought,
    InnerThoughtType innerThoughtType,
    TtsVoiceProvider ttsVoiceProvider,
    String ttsVoiceModel,
    String providerVoiceId,
    TtsVoiceGender ttsVoiceGender,
    BigDecimal bestStarRating) {}
