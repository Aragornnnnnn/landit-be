// AI 대화 생성 요청에 포함할 시나리오 컨텍스트를 담는다.

package com.landit.landitbe.feature.session.application.port;

/** AI 대화 생성 요청에 포함할 시나리오 컨텍스트를 담는다. */
public record AiScenarioContext(
    Long scenarioId,
    String title,
    String briefing,
    String conversationGoal,
    String counterpartRole,
    String serviceAudience) {}
