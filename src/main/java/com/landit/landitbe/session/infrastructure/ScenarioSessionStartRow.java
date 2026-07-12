// 시나리오 세션 시작 응답과 검증에 필요한 조회 결과를 담는다.
package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.content.domain.TtsVoiceGender;
import com.landit.landitbe.content.domain.TtsVoiceProvider;

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
        TtsVoiceGender ttsVoiceGender
) {
}
