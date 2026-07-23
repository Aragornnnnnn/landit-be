// 사용자 발화를 저장하고 AI 호출에 필요한 컨텍스트를 만든다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;
import com.landit.landitbe.feature.content.service.ScenarioContentService;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 사용자 발화를 저장하고 AI 호출에 필요한 컨텍스트를 만든다. */
@RequiredArgsConstructor
@Component
class SubmittedMessageService {

  private final LearningSessionService learningSessionService;
  private final ScenarioSessionService scenarioSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final SessionMessageService sessionMessageService;
  private final ScenarioContentService scenarioContentService;

  /** 사용자 메시지를 저장하고 AI 요청에 필요한 세션 컨텍스트를 반환한다. */
  SubmittedMessageContext record(
      long userId, long sessionId, String content, SessionMessageInputType inputType) {
    LearningSession learningSession =
        learningSessionService.findOwnedInProgressForUpdate(userId, sessionId);
    ScenarioSessionMessageContextProjection scenarioContext = findScenarioContext(sessionId);
    SessionHistoryLookup sessionHistoryLookup = findOrCreateSessionHistory(learningSession);
    SessionHistory sessionHistory = sessionHistoryLookup.sessionHistory();
    List<SessionHistoryMessage> previousMessages = findPreviousMessages(sessionHistory);

    int submittedTurnNumber = submittedTurnNumber(previousMessages);
    SessionHistoryMessage submittedMessage =
        saveUserMessage(sessionHistory, previousMessages, submittedTurnNumber, content, inputType);
    List<AiConversationHistoryMessage> conversationHistory =
        toConversationHistory(previousMessages, submittedMessage);

    Optional<ScenarioQuestionProjection> nextQuestion =
        findNextQuestion(
            learningSession,
            scenarioContext,
            nextQuestionOrder(scenarioContext, submittedTurnNumber));
    return new SubmittedMessageContext(
        userId,
        sessionId,
        learningSession.getId(),
        sessionHistory.getId(),
        submittedMessage.getId(),
        submittedMessage.getMessageSequence(),
        submittedMessage.getTurnNumber(),
        scenarioContext,
        conversationHistory,
        nextQuestion,
        sessionHistoryLookup.created());
  }

  /** AI 생성 실패 시 먼저 저장한 사용자 메시지를 제거한다. */
  void remove(SubmittedMessageContext submittedContext) {
    sessionMessageService.deleteIfExists(submittedContext.submittedMessageId());
    if (submittedContext.createdSessionHistory()) {
      sessionHistoryService.deleteIfExists(submittedContext.sessionHistoryId());
    }
  }

  private ScenarioSessionMessageContextProjection findScenarioContext(long sessionId) {
    return scenarioSessionService.requireMessageContext(sessionId);
  }

  private SessionHistoryLookup findOrCreateSessionHistory(LearningSession learningSession) {
    Optional<SessionHistory> sessionHistory =
        sessionHistoryService.findByLearningSessionId(learningSession.getId());
    if (sessionHistory.isPresent()) {
      return new SessionHistoryLookup(sessionHistory.get(), false);
    }
    return new SessionHistoryLookup(
        sessionHistoryService.save(
            SessionHistory.startedScenario(
                learningSession.getId(),
                learningSession.getUserProfileId(),
                learningSession.getTargetLocale(),
                learningSession.getBaseLocale(),
                learningSession.getStartedAt())),
        true);
  }

  private List<SessionHistoryMessage> findPreviousMessages(SessionHistory sessionHistory) {
    return sessionMessageService.findAll(sessionHistory.getId());
  }

  /** 기존 히스토리 기준으로 이번 사용자 메시지가 답변할 턴 번호를 계산한다. */
  private int submittedTurnNumber(List<SessionHistoryMessage> previousMessages) {
    if (previousMessages.isEmpty()) {
      return 1;
    }
    SessionHistoryMessage lastMessage = previousMessages.get(previousMessages.size() - 1);
    if (lastMessage.getRole() == ConversationSpeaker.USER) {
      throw new ApiException(ErrorCode.CONFLICT, "처리 중인 사용자 메시지가 있습니다.");
    }
    return lastMessage.getTurnNumber();
  }

  private SessionHistoryMessage saveUserMessage(
      SessionHistory sessionHistory,
      List<SessionHistoryMessage> previousMessages,
      int submittedTurnNumber,
      String content,
      SessionMessageInputType inputType) {
    SessionHistoryMessage submittedMessage =
        sessionMessageService.saveAndFlush(
            SessionHistoryMessage.user(
                sessionHistory.getId(),
                previousMessages.size() + 1,
                submittedTurnNumber,
                content,
                inputType));
    return submittedMessage;
  }

  /** AI first는 시작 질문 다음 순서부터, USER first는 첫 질문부터 조회한다. */
  private int nextQuestionOrder(
      ScenarioSessionMessageContextProjection scenarioContext, int submittedTurnNumber) {
    return scenarioContext.firstSpeaker() == ConversationSpeaker.AI
        ? submittedTurnNumber + 1
        : submittedTurnNumber;
  }

  private Optional<ScenarioQuestionProjection> findNextQuestion(
      LearningSession learningSession,
      ScenarioSessionMessageContextProjection scenarioContext,
      int nextQuestionOrder) {
    return scenarioContentService.findActiveQuestion(
        scenarioContext.scenarioId(),
        nextQuestionOrder,
        learningSession.getTargetLocale(),
        learningSession.getBaseLocale());
  }

  private List<AiConversationHistoryMessage> toConversationHistory(
      List<SessionHistoryMessage> previousMessages, SessionHistoryMessage submittedMessage) {
    List<SessionHistoryMessage> messages = new ArrayList<>(previousMessages);
    messages.add(submittedMessage);
    return messages.stream().map(this::toConversationHistoryMessage).toList();
  }

  private AiConversationHistoryMessage toConversationHistoryMessage(SessionHistoryMessage message) {
    return new AiConversationHistoryMessage(
        message.getId(),
        message.getTurnNumber(),
        message.getRole().name(),
        message.getContent(),
        message.getTranslatedContent());
  }

  private record SessionHistoryLookup(SessionHistory sessionHistory, boolean created) {}
}
