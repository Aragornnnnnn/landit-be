// AI 생성 결과를 세션 히스토리와 세션 상태에 반영한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitResponse.NextMessageResponse;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitResponse.SessionProgressResponse;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitResponse.SubmittedMessageResponse;
import com.landit.landitbe.session.domain.CompletionReason;
import com.landit.landitbe.session.domain.LearningSession;
import com.landit.landitbe.session.domain.ProcessingStatus;
import com.landit.landitbe.session.domain.ScenarioSession;
import com.landit.landitbe.session.domain.SessionHistory;
import com.landit.landitbe.session.domain.SessionHistoryMessage;
import com.landit.landitbe.session.infrastructure.ScenarioSessionRepository;
import com.landit.landitbe.session.infrastructure.SessionHistoryMessageRepository;
import com.landit.landitbe.session.infrastructure.SessionHistoryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class GeneratedMessageRecorder {

    private static final int AI_MESSAGE_SEQUENCE_IN_TURN = 2;

    private final LearningSessionFinder learningSessionFinder;
    private final ScenarioSessionRepository scenarioSessionRepository;
    private final SessionHistoryRepository sessionHistoryRepository;
    private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

    /** AI 생성 결과를 저장하고 사용자에게 반환할 메시지 제출 응답을 만든다. */
    SessionMessageSubmitResponse record(
            SubmittedMessageContext submittedContext,
            SessionMessageAiGenerator.Generation generation
    ) {
        LearningSession learningSession = learningSessionFinder.findOwnedInProgressForUpdate(
                submittedContext.userId(),
                submittedContext.sessionId()
        );
        ScenarioSession scenarioSession = findScenarioSession(submittedContext.sessionId());
        SessionHistory sessionHistory = findSessionHistory(submittedContext.sessionHistoryId());
        SessionHistoryMessage submittedMessage = findSubmittedMessage(submittedContext);
        assertSubmittedMessageMatches(submittedContext, submittedMessage);

        submittedMessage.recordInnerThought(
                generation.innerThought(),
                generation.innerThoughtType()
        );
        scenarioSession.updateGoalCompletionStatus(generation.goalCompletionStatus());
        SessionHistoryMessage nextMessage = saveAiMessage(
                sessionHistory,
                submittedMessage,
                generation.aiMessage(),
                generation.translatedMessage()
        );
        if (generation.completed()) {
            completeSession(learningSession, sessionHistory, generation.completionReason());
        }
        return toResponse(
                submittedContext.sessionId(),
                submittedMessage,
                new MessageGenerationResult(
                        nextMessage,
                        submittedContext.scenarioContext().totalQuestionCount(),
                        generation.completed()
                )
        );
    }

    private ScenarioSession findScenarioSession(long sessionId) {
        return scenarioSessionRepository.findByLearningSessionId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    }

    private SessionHistory findSessionHistory(Long sessionHistoryId) {
        return sessionHistoryRepository.findById(sessionHistoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    }

    private SessionHistoryMessage findSubmittedMessage(SubmittedMessageContext submittedContext) {
        return sessionHistoryMessageRepository.findById(submittedContext.submittedMessageId())
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void assertSubmittedMessageMatches(
            SubmittedMessageContext submittedContext,
            SessionHistoryMessage submittedMessage
    ) {
        if (submittedMessage.getRole() != ConversationSpeaker.USER
                || submittedMessage.getMessageSequence() != submittedContext.submittedMessageSequence()
                || submittedMessage.getTurnNumber() != submittedContext.submittedTurnNumber()) {
            throw new ApiException(ErrorCode.CONFLICT, "처리 중인 사용자 메시지가 변경되었습니다.");
        }
    }

    private SessionHistoryMessage saveAiMessage(
            SessionHistory sessionHistory,
            SessionHistoryMessage submittedMessage,
            String content,
            String translatedContent
    ) {
        return sessionHistoryMessageRepository.save(SessionHistoryMessage.aiGenerated(
                sessionHistory.getId(),
                submittedMessage.getMessageSequence() + 1,
                submittedMessage.getTurnNumber() + 1,
                content,
                translatedContent
        ));
    }

    private void completeSession(
            LearningSession learningSession,
            SessionHistory sessionHistory,
            CompletionReason completionReason
    ) {
        LocalDateTime endedAt = LocalDateTime.now();
        learningSession.completeBySystem(completionReason, endedAt);
        int userMessageCount = (int) sessionHistoryMessageRepository
                .findBySessionHistoryIdOrderByMessageSequenceAsc(sessionHistory.getId())
                .stream()
                .filter(message -> message.getRole() == ConversationSpeaker.USER)
                .count();
        sessionHistory.complete(endedAt, userMessageCount);
    }

    private SessionMessageSubmitResponse toResponse(
            long sessionId,
            SessionHistoryMessage submittedMessage,
            MessageGenerationResult generationResult
    ) {
        return new SessionMessageSubmitResponse(
                sessionId,
                toSubmittedMessageResponse(submittedMessage),
                toNextMessageResponse(generationResult.nextMessage()),
                new SessionProgressResponse(
                        generationResult.nextMessage().getTurnNumber(),
                        AI_MESSAGE_SEQUENCE_IN_TURN,
                        generationResult.totalQuestionCount(),
                        generationResult.completed()
                )
        );
    }

    private SubmittedMessageResponse toSubmittedMessageResponse(SessionHistoryMessage message) {
        return new SubmittedMessageResponse(
                message.getId(),
                message.getTurnNumber(),
                message.getMessageSequence(),
                message.getRole().name(),
                ProcessingStatus.PREPARING.name(),
                message.getInnerThought(),
                message.getInnerThoughtType() == null ? null : message.getInnerThoughtType().name()
        );
    }

    private NextMessageResponse toNextMessageResponse(SessionHistoryMessage message) {
        return new NextMessageResponse(
                message.getId(),
                message.getTurnNumber(),
                message.getMessageSequence(),
                message.getRole().name(),
                message.getContent(),
                message.getTranslatedContent()
        );
    }

    private record MessageGenerationResult(
            SessionHistoryMessage nextMessage,
            int totalQuestionCount,
            boolean completed
    ) {
    }
}
