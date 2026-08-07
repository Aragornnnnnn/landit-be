// 최종 피드백 생성에 필요한 완료 세션 데이터를 보관한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiScenarioContext;
import com.landit.landitbe.shared.domain.Locale;
import java.util.List;
import java.util.Optional;

/** 최종 피드백 생성에 필요한 완료 세션 데이터를 보관한다. */
record LoadedSessionFeedbackContext(
    Long sessionId,
    Long sessionHistoryId,
    Locale targetLocale,
    Locale baseLocale,
    AiScenarioContext scenario,
    List<UserMessageContext> userMessages,
    Optional<ExistingSummaryFeedbackContext> existingSummary) {}
