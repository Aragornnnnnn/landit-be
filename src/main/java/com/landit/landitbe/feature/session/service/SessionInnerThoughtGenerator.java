// 사용자 발화에 대한 AI 속마음을 생성한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiInnerThoughtRequest;
import com.landit.landitbe.feature.session.client.ai.AiInnerThoughtResult;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 사용자 발화에 대한 AI 속마음을 생성한다. */
@RequiredArgsConstructor
@Component
class SessionInnerThoughtGenerator {

  private final AiConversationClient aiConversationClient;
  private final AiScenarioContextMapper aiScenarioContextMapper;

  /** 사용자 발화 컨텍스트로 AI 속마음을 생성하고 응답 식별자를 검증한다. */
  AiInnerThoughtResult generate(SubmittedMessageContext submittedContext) {
    AiInnerThoughtRequest request =
        new AiInnerThoughtRequest(
            submittedContext.learningSessionId(),
            submittedContext.submittedMessageId(),
            submittedContext.submittedTurnNumber(),
            aiScenarioContextMapper.map(submittedContext.scenarioContext()),
            submittedContext.conversationHistory());
    AiInnerThoughtResult result = aiConversationClient.generateInnerThought(request);
    if (result == null
        || !request.sessionId().equals(result.sessionId())
        || !request.submittedMessageId().equals(result.messageId())
        || result.innerThought() == null
        || result.innerThought().isBlank()
        || result.innerThoughtType() == null) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    return result;
  }
}
