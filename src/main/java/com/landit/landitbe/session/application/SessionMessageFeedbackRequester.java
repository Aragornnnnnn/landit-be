// 사용자 메시지에 대한 AI 피드백 생성을 요청하고 접수 결과를 검증한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.application.port.AiConversationClient;
import com.landit.landitbe.session.application.port.AiConversationHistoryMessage;
import com.landit.landitbe.session.application.port.AiMessageFeedbackContext;
import com.landit.landitbe.session.application.port.AiMessageFeedbackRequest;
import com.landit.landitbe.session.application.port.AiMessageFeedbackResult;
import com.landit.landitbe.session.domain.ProcessingStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class SessionMessageFeedbackRequester {

    private final AiConversationClient aiConversationClient;
    private final AiScenarioContextMapper aiScenarioContextMapper;

    /** 직전 AI 메시지가 있는 사용자 메시지의 피드백 생성을 요청한다. */
    ProcessingStatus request(SubmittedMessageContext submittedContext) {
        Optional<AiConversationHistoryMessage> precedingAiMessage = findPrecedingAiMessage(
                submittedContext.conversationHistory()
        );
        if (precedingAiMessage.isEmpty()) {
            return null;
        }

        AiMessageFeedbackRequest request = toRequest(submittedContext, precedingAiMessage.get());
        AiMessageFeedbackResult result = aiConversationClient.requestMessageFeedback(request);
        validateResult(result, request);
        return result.feedbackStatus();
    }

    private Optional<AiConversationHistoryMessage> findPrecedingAiMessage(
            List<AiConversationHistoryMessage> conversationHistory
    ) {
        if (conversationHistory.size() < 2) {
            return Optional.empty();
        }
        AiConversationHistoryMessage precedingMessage = conversationHistory.get(
                conversationHistory.size() - 2
        );
        return "AI".equals(precedingMessage.role())
                ? Optional.of(precedingMessage)
                : Optional.empty();
    }

    private AiMessageFeedbackRequest toRequest(
            SubmittedMessageContext submittedContext,
            AiConversationHistoryMessage precedingAiMessage
    ) {
        return new AiMessageFeedbackRequest(
                submittedContext.learningSessionId(),
                submittedContext.submittedMessageId(),
                submittedContext.submittedTurnNumber(),
                submittedContext.submittedMessageSequence(),
                aiScenarioContextMapper.map(submittedContext.scenarioContext()),
                new AiMessageFeedbackContext(
                        precedingAiMessage.content(),
                        precedingAiMessage.translatedContent(),
                        submittedContext.conversationHistory().getLast().content()
                )
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
