// 시나리오 세션 시작 응답과 검증에 필요한 조회 결과를 담는다.

package com.landit.landitbe.feature.session.infrastructure;

import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;

/** 시나리오 세션 시작 응답과 검증에 필요한 조회 결과를 담는다. */
public record ScenarioSessionStartRow(
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
