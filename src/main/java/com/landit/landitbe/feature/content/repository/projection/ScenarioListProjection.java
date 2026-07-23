// 시나리오 목록 조회 결과 한 행을 담는 JPA projection record다.

package com.landit.landitbe.feature.content.repository.projection;

import com.landit.landitbe.feature.content.domain.ScenarioDifficulty;
import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.feature.learning.domain.UserScenarioProgressStatus;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.math.BigDecimal;

/** 시나리오 목록 조회 결과 한 행을 담는 JPA projection record다. */
public record ScenarioListProjection(
    Long categoryId,
    String categoryName,
    int categoryDisplayOrder,
    ActiveStatus categoryStatus,
    Long scenarioId,
    int scenarioDisplayOrder,
    String scenarioTitle,
    String briefing,
    String conversationGoal,
    ScenarioDifficulty difficulty,
    ConversationSpeaker firstSpeaker,
    String thumbnailUrl,
    ActiveStatus scenarioStatus,
    ActiveStatus variantStatus,
    String aiOpeningMessage,
    String aiOpeningMessageTranslation,
    String userOpeningInstruction,
    String innerThought,
    InnerThoughtType innerThoughtType,
    TtsVoiceProvider ttsVoiceProvider,
    String ttsVoiceModel,
    String providerVoiceId,
    TtsVoiceGender ttsVoiceGender,
    UserScenarioProgressStatus progressStatus,
    BigDecimal bestStarRating) {}
