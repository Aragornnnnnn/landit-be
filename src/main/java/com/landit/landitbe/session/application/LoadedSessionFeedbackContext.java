// 최종 피드백 생성에 필요한 완료 세션 데이터를 보관한다.

package com.landit.landitbe.session.application;

import com.landit.landitbe.common.domain.Locale;
import com.landit.landitbe.session.application.port.AiScenarioContext;
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
