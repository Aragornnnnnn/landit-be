// 사용자 메시지에 대한 AI 피드백 생성을 요청하고 접수 결과를 검증한다.

package com.landit.landitbe.feature.session.scenario.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationSettings;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.scenario.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.scenario.client.ai.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.feature.session.scenario.client.ai.AiMessageFeedbackRequest;
import com.landit.landitbe.feature.session.scenario.client.ai.AiMessageFeedbackResult;
import com.landit.landitbe.feature.session.scenario.client.ai.AiScenarioContext;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 사용자 메시지에 대한 AI 피드백 생성을 요청하고 접수 결과를 검증한다. */
@RequiredArgsConstructor
@Component
class SessionMessageFeedbackRequester {

  private final AiConversationClient aiConversationClient;
  private final AiConversationSettings aiConversationSettings;
  private final SessionMessageService sessionMessageService;

  /** 사용자 메시지의 평가 기준을 구성해 피드백 생성을 요청한다. */
  ProcessingStatus request(SubmittedMessageContext submittedContext) {
    AiMessageFeedbackRequest request = toRequest(submittedContext);
    AiMessageFeedbackResult result = aiConversationClient.requestMessageFeedback(request);
    validateResult(result, request);
    if (result.feedbackStatus() == ProcessingStatus.FAILED) {
      sessionMessageService.failFeedback(submittedContext.submittedMessageId());
    }
    return result.feedbackStatus();
  }

  /** AI First와 USER First 시작 발화에 맞는 평가 기준을 요청 본문으로 조립한다. */
  private AiMessageFeedbackRequest toRequest(SubmittedMessageContext submittedContext) {
    return new AiMessageFeedbackRequest(
        submittedContext.learningSessionId(),
        submittedContext.submittedMessageId(),
        submittedContext.submittedTurnNumber(),
        submittedContext.submittedMessageSequence(),
        AiScenarioContext.from(submittedContext.scenarioContext(), aiConversationSettings),
        evaluationContext(submittedContext),
        submittedContext.conversationHistory().getLast().content());
  }

  /** USER First 첫 발화는 시작 안내로, 나머지 발화는 직전 AI 메시지로 평가한다. */
  private AiMessageFeedbackEvaluationContext evaluationContext(
      SubmittedMessageContext submittedContext) {
    return AiMessageFeedbackEvaluationContext.from(
        submittedContext.scenarioContext(), submittedContext.conversationHistory());
  }

  private void validateResult(AiMessageFeedbackResult result, AiMessageFeedbackRequest request) {
    if (result == null
        || !request.sessionId().equals(result.sessionId())
        || !request.messageId().equals(result.messageId())
        || (result.feedbackStatus() != ProcessingStatus.PREPARING
            && result.feedbackStatus() != ProcessingStatus.FAILED)) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
  }
}
