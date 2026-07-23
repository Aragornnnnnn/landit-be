// AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.

package com.landit.landitbe.feature.session.application.port;

import java.util.List;

/** AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다. */
public record AiSessionFeedbackRequest(
    Long sessionId, AiScenarioContext scenario, List<Long> expectedMessageIds) {}
