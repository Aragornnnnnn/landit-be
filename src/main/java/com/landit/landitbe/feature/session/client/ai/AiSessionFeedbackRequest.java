// AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.content.domain.ResponseDemand;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param scenario AI 요청용 시나리오 컨텍스트
 * @param expectedMessageIds 피드백을 생성할 메시지 ID 목록
 * @param assessmentMessages 수준 평가용 사용자 원문과 질문 정보
 * @param completedFeedbacks 저장된 메시지 평가 결과. 구 AI 결과만 있으면 null
 */
public record AiSessionFeedbackRequest(
    Long sessionId,
    AiScenarioContext scenario,
    List<Long> expectedMessageIds,
    List<AssessmentMessage> assessmentMessages,
    List<JsonNode> completedFeedbacks) {

  /** 기존 수준 평가 요청은 메시지 결과를 별도로 전달하지 않는다. */
  public AiSessionFeedbackRequest(
      Long sessionId,
      AiScenarioContext scenario,
      List<Long> expectedMessageIds,
      List<AssessmentMessage> assessmentMessages) {
    this(sessionId, scenario, expectedMessageIds, assessmentMessages, null);
  }

  /** 수준 평가 입력 없이 기존 최종 피드백만 요청한다. */
  public AiSessionFeedbackRequest(
      Long sessionId, AiScenarioContext scenario, List<Long> expectedMessageIds) {
    this(sessionId, scenario, expectedMessageIds, List.of(), null);
  }

  /** 질문별 수준 평가에 필요한 사용자 원문과 메타데이터다. */
  public record AssessmentMessage(
      Long messageId,
      String evaluationContext,
      String userMessage,
      ResponseDemand responseDemand,
      List<String> requiredElements) {}
}
