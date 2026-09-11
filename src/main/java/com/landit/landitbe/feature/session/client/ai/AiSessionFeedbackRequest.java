// AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.content.domain.ResponseDemand;
import java.util.List;

/**
 * AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param scenario AI 요청용 시나리오 컨텍스트
 * @param expectedMessageIds 피드백을 생성할 메시지 ID 목록
 */
public record AiSessionFeedbackRequest(
    Long sessionId,
    AiScenarioContext scenario,
    List<Long> expectedMessageIds,
    List<AssessmentMessage> assessmentMessages) {

  /** 수준 평가 입력 없이 기존 최종 피드백만 요청한다. */
  public AiSessionFeedbackRequest(
      Long sessionId, AiScenarioContext scenario, List<Long> expectedMessageIds) {
    this(sessionId, scenario, expectedMessageIds, List.of());
  }

  /** 질문별 수준 평가에 필요한 사용자 원문과 메타데이터다. */
  public record AssessmentMessage(
      Long messageId,
      String evaluationContext,
      String userMessage,
      ResponseDemand responseDemand,
      List<String> requiredElements) {}
}
