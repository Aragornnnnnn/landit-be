// AI 생성 결과를 세션 히스토리와 세션 상태에 반영한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.ScenarioSession;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.feature.session.repository.ScenarioSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** AI 생성 결과를 세션 히스토리와 세션 상태에 반영한다. */
@RequiredArgsConstructor
@Component
class GeneratedMessageRecorder {

  private final LearningSessionFinder learningSessionFinder;
  private final ScenarioSessionRepository scenarioSessionRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** AI 생성 결과를 저장하고 사용자에게 반환할 메시지 제출 응답을 만든다. */
  SessionMessageSubmitResponse record(
      SubmittedMessageContext submittedContext,
      SessionMessageAiGenerator.Generation generation,
      ProcessingStatus feedbackProcessingStatus) {
    final LearningSession learningSession =
        learningSessionFinder.findOwnedInProgressForUpdate(
            submittedContext.userId(), submittedContext.sessionId());
    ScenarioSession scenarioSession = findScenarioSession(submittedContext.sessionId());
    SessionHistoryMessage submittedMessage = findSubmittedMessage(submittedContext);
    assertSubmittedMessageMatches(submittedContext, submittedMessage);

    if (generation.completed()) {
      submittedMessage.recordInnerThought(generation.innerThought(), generation.innerThoughtType());
    }
    scenarioSession.updateGoalCompletionStatus(generation.goalCompletionStatus());
    SessionHistoryMessage nextMessage =
        saveAiMessage(submittedMessage, generation.aiMessage(), generation.translatedMessage());
    if (generation.completed()) {
      learningSession.completeBySystem(generation.completionReason(), LocalDateTime.now());
    }
    return SessionMessageSubmitResponse.from(
        submittedContext.sessionId(),
        submittedMessage,
        feedbackProcessingStatus,
        nextMessage,
        submittedContext.scenarioContext().totalQuestionCount(),
        generation.completed());
  }

  private ScenarioSession findScenarioSession(long sessionId) {
    return scenarioSessionRepository
        .findByLearningSessionId(sessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  private SessionHistoryMessage findSubmittedMessage(SubmittedMessageContext submittedContext) {
    return sessionHistoryMessageRepository
        .findById(submittedContext.submittedMessageId())
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  private void assertSubmittedMessageMatches(
      SubmittedMessageContext submittedContext, SessionHistoryMessage submittedMessage) {
    if (submittedMessage.getRole() != ConversationSpeaker.USER
        || submittedMessage.getMessageSequence() != submittedContext.submittedMessageSequence()
        || submittedMessage.getTurnNumber() != submittedContext.submittedTurnNumber()) {
      throw new ApiException(ErrorCode.CONFLICT, "처리 중인 사용자 메시지가 변경되었습니다.");
    }
  }

  private SessionHistoryMessage saveAiMessage(
      SessionHistoryMessage submittedMessage, String content, String translatedContent) {
    return sessionHistoryMessageRepository.save(
        SessionHistoryMessage.aiGenerated(
            submittedMessage.getSessionHistoryId(),
            submittedMessage.getMessageSequence() + 1,
            submittedMessage.getTurnNumber() + 1,
            content,
            translatedContent));
  }
}
