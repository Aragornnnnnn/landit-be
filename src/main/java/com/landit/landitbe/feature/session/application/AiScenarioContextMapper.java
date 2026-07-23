// 세션 조회 결과를 AI 요청용 시나리오 컨텍스트로 변환한다.

package com.landit.landitbe.feature.session.application;

import com.landit.landitbe.feature.session.application.port.AiConversationSettings;
import com.landit.landitbe.feature.session.application.port.AiScenarioContext;
import com.landit.landitbe.feature.session.infrastructure.ScenarioSessionMessageContextRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 세션 조회 결과를 AI 요청용 시나리오 컨텍스트로 변환한다. */
@RequiredArgsConstructor
@Component
class AiScenarioContextMapper {

  private final AiConversationSettings aiConversationSettings;

  /** 세션 시나리오 조회 결과를 AI 요청 컨텍스트로 변환한다. */
  AiScenarioContext map(ScenarioSessionMessageContextRow scenarioContext) {
    return new AiScenarioContext(
        scenarioContext.scenarioId(),
        scenarioContext.title(),
        scenarioContext.briefing(),
        scenarioContext.conversationGoal(),
        scenarioContext.counterpartRole(),
        aiConversationSettings.serviceAudience());
  }
}
