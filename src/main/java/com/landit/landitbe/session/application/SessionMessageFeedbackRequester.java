// 사용자 메시지에 대한 AI 피드백 생성을 요청하고 접수 결과를 검증한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.session.application.port.AiConversationClient;
import com.landit.landitbe.session.application.port.AiConversationHistoryMessage;
import com.landit.landitbe.session.application.port.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.session.application.port.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.session.application.port.AiMessageFeedbackRequest;
import com.landit.landitbe.session.application.port.AiMessageFeedbackResult;
import com.landit.landitbe.session.domain.ProcessingStatus;
import com.landit.landitbe.session.infrastructure.ScenarioSessionMessageContextRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class SessionMessageFeedbackRequester {

    private final AiConversationClient aiConversationClient;
    private final AiScenarioContextMapper aiScenarioContextMapper;

    /** 사용자 메시지의 평가 기준을 구성해 피드백 생성을 요청한다. */
    ProcessingStatus request(SubmittedMessageContext submittedContext) {
        AiMessageFeedbackRequest request = toRequest(submittedContext);
        AiMessageFeedbackResult result = aiConversationClient.requestMessageFeedback(request);
        validateResult(result, request);
        return result.feedbackStatus();
    }

    /** AI First와 USER First 시작 발화에 맞는 평가 기준을 요청 본문으로 조립한다. */
    private AiMessageFeedbackRequest toRequest(SubmittedMessageContext submittedContext) {
        return new AiMessageFeedbackRequest(
                submittedContext.learningSessionId(),
                submittedContext.submittedMessageId(),
                submittedContext.submittedTurnNumber(),
                submittedContext.submittedMessageSequence(),
                aiScenarioContextMapper.map(submittedContext.scenarioContext()),
                evaluationContext(submittedContext),
                submittedContext.conversationHistory().getLast().content()
        );
    }

    /** USER First 첫 발화는 시작 안내로, 나머지 발화는 직전 AI 메시지로 평가한다. */
    private AiMessageFeedbackEvaluationContext evaluationContext(
            SubmittedMessageContext submittedContext
    ) {
        return evaluationContext(
                submittedContext.scenarioContext(),
                submittedContext.conversationHistory()
        );
    }

    /** 시나리오 시작 발화와 직전 AI 메시지에 맞는 평가 기준을 조립한다. */
    static AiMessageFeedbackEvaluationContext evaluationContext(
            ScenarioSessionMessageContextRow scenarioContext,
            List<AiConversationHistoryMessage> conversationHistory
    ) {
        if (isUserFirstOpeningMessage(scenarioContext, conversationHistory)) {
            return scenarioOpeningInstructionContext(scenarioContext);
        }
        return precedingAiMessageContext(conversationHistory);
    }

    /** USER First 시나리오의 첫 사용자 메시지인지 판별한다. */
    private static boolean isUserFirstOpeningMessage(
            ScenarioSessionMessageContextRow scenarioContext,
            List<AiConversationHistoryMessage> conversationHistory
    ) {
        return scenarioContext.firstSpeaker() == ConversationSpeaker.USER
                && conversationHistory.size() == 1;
    }

    /** USER First 시작 안내를 평가 기준으로 변환한다. */
    private static AiMessageFeedbackEvaluationContext scenarioOpeningInstructionContext(
            ScenarioSessionMessageContextRow scenarioContext
    ) {
        String instruction = scenarioContext.userOpeningInstruction();
        if (instruction == null || instruction.isBlank()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new AiMessageFeedbackEvaluationContext(
                AiMessageFeedbackEvaluationContextType.SCENARIO_OPENING_INSTRUCTION,
                instruction,
                null
        );
    }

    /** 직전 AI 메시지를 사용자 발화의 평가 기준으로 변환한다. */
    private static AiMessageFeedbackEvaluationContext precedingAiMessageContext(
            List<AiConversationHistoryMessage> conversationHistory
    ) {
        if (conversationHistory.size() < 2) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        AiConversationHistoryMessage precedingMessage = conversationHistory.get(
                conversationHistory.size() - 2
        );
        if (!ConversationSpeaker.AI.name().equals(precedingMessage.role())) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new AiMessageFeedbackEvaluationContext(
                AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
                precedingMessage.content(),
                precedingMessage.translatedContent()
        );
    }

    private void validateResult(
            AiMessageFeedbackResult result,
            AiMessageFeedbackRequest request
    ) {
        if (result == null
                || !request.sessionId().equals(result.sessionId())
                || !request.messageId().equals(result.messageId())
                || result.feedbackStatus() != ProcessingStatus.PREPARING) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }
}
