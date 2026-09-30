// AI 세션 최종 피드백 생성을 요청하는 본문을 표현한다.

package com.landit.landitbe.feature.learning.scenario.feedback.client.ai;

import com.landit.landitbe.feature.content.scenario.question.domain.ResponseDemand;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.LoadedSessionFeedbackContext;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.UserMessageContext;
import com.landit.landitbe.feature.learning.scenario.session.client.ai.AiScenarioContext;
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
 * @param previousMistakes 직전 완료 시나리오에서 실제 교정된 발화
 * @param learnedExpressions 사용 여부를 판정할 학습 완료 표현
 */
public record AiSessionFeedbackRequest(
    Long sessionId,
    AiScenarioContext scenario,
    List<Long> expectedMessageIds,
    List<AssessmentMessage> assessmentMessages,
    List<JsonNode> completedFeedbacks,
    List<PreviousMistake> previousMistakes,
    List<LearnedExpression> learnedExpressions) {

  /**
   * 기존 호출자는 비교·재사용 근거 없이 요약을 요청한다.
   *
   * @param sessionId 학습 세션 ID
   * @param scenario AI 요청용 시나리오 컨텍스트
   * @param expectedMessageIds 피드백을 생성할 메시지 ID 목록
   * @param assessmentMessages 수준 평가용 사용자 원문과 질문 정보
   * @param completedFeedbacks 저장된 메시지 평가 결과
   */
  public AiSessionFeedbackRequest(
      Long sessionId,
      AiScenarioContext scenario,
      List<Long> expectedMessageIds,
      List<AssessmentMessage> assessmentMessages,
      List<JsonNode> completedFeedbacks) {
    this(
        sessionId,
        scenario,
        expectedMessageIds,
        assessmentMessages,
        completedFeedbacks,
        List.of(),
        List.of());
  }

  /**
   * 기존 수준 평가 요청은 메시지 결과를 별도로 전달하지 않는다.
   *
   * @param sessionId 학습 세션 ID
   * @param scenario AI 요청용 시나리오 컨텍스트
   * @param expectedMessageIds 피드백을 생성할 메시지 ID 목록
   * @param assessmentMessages 수준 평가용 사용자 원문과 질문 정보
   */
  public AiSessionFeedbackRequest(
      Long sessionId,
      AiScenarioContext scenario,
      List<Long> expectedMessageIds,
      List<AssessmentMessage> assessmentMessages) {
    this(sessionId, scenario, expectedMessageIds, assessmentMessages, null);
  }

  /**
   * 수준 평가 입력 없이 기존 최종 피드백만 요청한다.
   *
   * @param sessionId 학습 세션 ID
   * @param scenario AI 요청용 시나리오 컨텍스트
   * @param expectedMessageIds 피드백을 생성할 메시지 ID 목록
   */
  public AiSessionFeedbackRequest(
      Long sessionId, AiScenarioContext scenario, List<Long> expectedMessageIds) {
    this(sessionId, scenario, expectedMessageIds, List.of(), null);
  }

  /**
   * 질문별 수준 평가에 필요한 사용자 원문과 메타데이터다.
   *
   * @param messageId 사용자 메시지 ID
   * @param evaluationContext 평가 기준 문맥
   * @param userMessage 사용자 원문
   * @param responseDemand 기대 응답 수준
   * @param requiredElements 답변에 필요한 요소
   */
  public record AssessmentMessage(
      Long messageId,
      String evaluationContext,
      String userMessage,
      ResponseDemand responseDemand,
      List<String> requiredElements) {}

  /**
   * 직전 완료 시나리오의 실제 교정 정보다.
   *
   * @param messageId 직전 사용자 메시지 ID
   * @param userMessage 교정 당시 사용자 원문
   * @param correctionExpression 교정 표현
   * @param correctionReason 교정 이유
   */
  public record PreviousMistake(
      Long messageId, String userMessage, String correctionExpression, String correctionReason) {}

  /**
   * 표현 재사용 여부를 판정할 학습 언어 표현과 뜻이다.
   *
   * @param expressionId 학습 표현 ID
   * @param text 학습 표현 원문
   * @param meaning 표현의 기준 언어 뜻
   */
  public record LearnedExpression(Long expressionId, String text, String meaning) {}

  /**
   * 완료 세션 컨텍스트를 AI 수준 평가 요청으로 변환한다.
   *
   * @param context 완료 세션의 평가 근거
   * @return 수준 평가 요청. 최종 피드백 전용 필드는 포함하지 않는다
   */
  public static AiSessionFeedbackRequest forLevelAssessment(LoadedSessionFeedbackContext context) {
    return new AiSessionFeedbackRequest(
        context.sessionId(),
        context.scenario(),
        context.userMessages().stream().map(UserMessageContext::messageId).toList(),
        context.userMessages().stream()
            .map(
                message ->
                    new AiSessionFeedbackRequest.AssessmentMessage(
                        message.messageId(),
                        message.evaluationContext().content(),
                        message.content(),
                        message.responseDemand(),
                        message.requiredElements()))
            .toList());
  }
}
