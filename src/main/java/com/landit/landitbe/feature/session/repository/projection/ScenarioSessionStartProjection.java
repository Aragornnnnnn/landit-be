// 시나리오 세션 시작 응답과 검증에 필요한 조회 결과를 담는다.

package com.landit.landitbe.feature.session.repository.projection;

import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;

/**
 * 시나리오 세션 시작 응답과 검증에 필요한 조회 결과를 담는다.
 *
 * @param scenarioId 시나리오 ID
 * @param categoryId 카테고리 ID
 * @param categoryStatus 카테고리 활성 상태
 * @param scenarioStatus 시나리오 활성 상태
 * @param variantId 시나리오 언어 Variant ID
 * @param variantStatus 시나리오 언어 Variant 활성 상태
 * @param firstSpeaker 첫 발화자
 * @param totalQuestionCount 전체 질문 수
 * @param userOpeningInstruction 사용자 첫 발화 안내
 * @param aiOpeningMessage 첫 AI 메시지
 * @param aiOpeningMessageTranslation 첫 AI 메시지 번역
 * @param aiOpeningInnerThought 첫 AI 메시지의 속마음
 * @param aiOpeningInnerThoughtType 첫 AI 메시지의 속마음 유형
 * @param ttsVoiceProvider TTS 제공자
 * @param ttsVoiceModel TTS 음성 모델
 * @param providerVoiceId TTS 제공자 음성 ID
 * @param ttsVoiceGender TTS 음성 성별
 */
public record ScenarioSessionStartProjection(
    Long scenarioId,
    Long categoryId,
    ActiveStatus categoryStatus,
    ActiveStatus scenarioStatus,
    Long variantId,
    ActiveStatus variantStatus,
    ConversationSpeaker firstSpeaker,
    int totalQuestionCount,
    String userOpeningInstruction,
    String aiOpeningMessage,
    String aiOpeningMessageTranslation,
    String aiOpeningInnerThought,
    InnerThoughtType aiOpeningInnerThoughtType,
    TtsVoiceProvider ttsVoiceProvider,
    String ttsVoiceModel,
    String providerVoiceId,
    TtsVoiceGender ttsVoiceGender) {}
