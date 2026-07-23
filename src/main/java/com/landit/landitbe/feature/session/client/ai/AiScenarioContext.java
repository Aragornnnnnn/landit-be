// AI 대화 생성 요청에 포함할 시나리오 컨텍스트를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;

/**
 * AI 대화 생성 요청에 포함할 시나리오 컨텍스트를 담는다.
 *
 * @param scenarioId 시나리오 ID
 * @param title 제목
 * @param briefing 시나리오 설명
 * @param conversationGoal 대화 목표
 * @param counterpartRole 상대 발화자 역할
 * @param serviceAudience AI 서비스 대상 사용자군
 */
public record AiScenarioContext(
    Long scenarioId,
    String title,
    String briefing,
    String conversationGoal,
    String counterpartRole,
    String serviceAudience) {

  /**
   * 세션 시나리오 Projection과 AI 설정을 요청 컨텍스트로 변환한다.
   *
   * @param projection 세션 시나리오 조회 결과
   * @param settings AI 대화 설정
   * @return AI 대화 생성용 시나리오 컨텍스트
   */
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
