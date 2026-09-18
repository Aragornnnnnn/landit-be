// 사용자 메시지에 대한 AI 피드백 생성을 요청하고 접수 결과를 검증한다.

package com.landit.landitbe.feature.learning.scenario.session.message.service;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationSettings;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.scenario.session.client.ai.AiScenarioContext;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackRequest;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.service.MessageFeedbackWorkService;
import com.landit.landitbe.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 사용자 메시지에 대한 AI 피드백 생성을 요청하고 접수 결과를 검증한다. */
@RequiredArgsConstructor
@Component
class SessionMessageFeedbackRequester {

  private final MessageFeedbackWorkService feedbackWorkService;
  private final ConversationMessageService conversationMessageService;
  private final AiConversationSettings aiConversationSettings;

  /** 사용자 메시지의 평가 기준을 구성해 피드백 생성을 요청한다. */
  ProcessingStatus request(SubmittedMessageContext submittedContext) {
    return feedbackWorkService.generate(submittedContext.submittedMessageId());
  }

  /** 사용자 발화와 함께 평가 입력을 저장해 프로세스 종료 뒤에도 재실행한다. */
  void prepare(SubmittedMessageContext submittedContext) {
    try {
      feedbackWorkService.prepare(toRequest(submittedContext));
    } catch (ApiException exception) {
      // 평가 기준이 빠져도 대화의 다음 질문 생성은 계속한다.
      conversationMessageService.failFeedback(submittedContext.submittedMessageId());
    }
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
}
