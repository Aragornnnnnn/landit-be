// 시나리오 목록 조회 결과 한 행을 담는 JPA projection record다.
package com.landit.landitbe.content.infrastructure;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.content.domain.ScenarioDifficulty;
import com.landit.landitbe.learning.domain.UserScenarioProgressStatus;
import java.math.BigDecimal;

public record ScenarioListRow(
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
        String ttsVoiceSetId,
        UserScenarioProgressStatus progressStatus,
        BigDecimal bestStarRating
) {
}
