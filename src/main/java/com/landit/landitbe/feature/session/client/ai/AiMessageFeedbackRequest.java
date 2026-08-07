// AI 메시지별 피드백 생성을 요청하는 본문을 표현한다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * AI 메시지별 피드백 생성을 요청하는 본문을 표현한다.
 *
 * @param sessionId 학습 세션 ID
 * @param messageId 메시지 ID
 * @param turnNumber 대화 턴 번호
 * @param messageSequence 세션 내 메시지 순서
 * @param scenario AI 요청용 시나리오 컨텍스트
 * @param evaluationContext 메시지 평가 기준
 * @param userMessage 사용자 메시지
 */
public record AiMessageFeedbackRequest(
    Long sessionId,
    Long messageId,
    int turnNumber,
    int messageSequence,
    AiScenarioContext scenario,
    AiMessageFeedbackEvaluationContext evaluationContext,
    String userMessage) {}
