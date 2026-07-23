// 사용자 발화 제출에 필요한 시나리오 컨텍스트 조회 결과를 담는다.

package com.landit.landitbe.feature.session.infrastructure;

import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.Locale;

/** 사용자 발화 제출에 필요한 시나리오 컨텍스트 조회 결과를 담는다. */
public record ScenarioSessionMessageContextRow(
    Long scenarioId,
    String title,
    String briefing,
    String conversationGoal,
    String counterpartRole,
    ConversationSpeaker firstSpeaker,
    String userOpeningInstruction,
    int totalQuestionCount,
    Locale targetLocale,
    Locale baseLocale) {}
