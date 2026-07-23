// AI 대화 생성 요청에 포함할 시나리오 컨텍스트를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;

/** AI 대화 생성 요청에 포함할 시나리오 컨텍스트를 담는다. */
public record AiScenarioContext(
    Long scenarioId,
    String title,
    String briefing,
    String conversationGoal,
    String counterpartRole,
    String serviceAudience) {

  /** 세션 시나리오 Projection과 AI 설정을 요청 컨텍스트로 변환한다. */
  public static AiScenarioContext from(
      ScenarioSessionMessageContextProjection projection, AiConversationSettings settings) {
    return new AiScenarioContext(
        projection.scenarioId(),
        projection.title(),
        projection.briefing(),
        projection.conversationGoal(),
        projection.counterpartRole(),
        settings.serviceAudience());
  }
}
