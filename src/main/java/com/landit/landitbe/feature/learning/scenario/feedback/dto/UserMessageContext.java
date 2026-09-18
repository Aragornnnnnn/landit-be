// 사용자 메시지와 평가 컨텍스트를 보관한다.

package com.landit.landitbe.feature.learning.scenario.feedback.dto;

import com.landit.landitbe.feature.content.scenario.question.domain.ResponseDemand;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackEvaluationContext;
import java.util.List;

/**
 * 사용자 메시지와 평가 컨텍스트를 보관한다.
 *
 * @param messageId 발화 ID
 * @param turnNumber 대화 턴 번호
 * @param content 사용자 발화 원문
 * @param evaluationContext 평가 기준 질문
 * @param responseDemand 응답 요구 수준
 * @param requiredElements 필수 응답 요소
 */
public record UserMessageContext(
    Long messageId,
    int turnNumber,
    String content,
    AiMessageFeedbackEvaluationContext evaluationContext,
    ResponseDemand responseDemand,
    List<String> requiredElements) {}
