// 사용자 발화를 저장하고 AI 호출에 필요한 컨텍스트를 만든다.

package com.landit.landitbe.feature.session.scenario.message.service;

import com.landit.landitbe.feature.content.scenario.question.dto.NextQuestionContext;
import com.landit.landitbe.feature.content.scenario.service.ScenarioContentService;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import com.landit.landitbe.feature.session.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.session.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.session.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.session.history.service.ConversationMessageService;
import com.landit.landitbe.feature.session.history.service.SessionHistoryService;
import com.landit.landitbe.feature.session.scenario.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.feature.session.scenario.service.ScenarioSessionService;
import com.landit.landitbe.feature.session.service.LearningSessionService;
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

  private final com.landit.landitbe.config.ai.AiClientProperties aiProperties;
  private final java.time.Clock clock;
  private final tools.jackson.databind.json.JsonMapper mapper;
  private final LearningSessionService learningSessionService;
  private final ScenarioSessionService scenarioSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final ConversationMessageService conversationMessageService;
  private final ScenarioContentService scenarioContentService;

  /** 사용자 메시지를 저장하고 AI 요청에 필요한 세션 컨텍스트를 반환한다. */
  SubmittedMessageContext record(
      long userId,
      long sessionId,
      String content,
      SessionMessageInputType inputType,
      String clientMessageId) {
    LearningSessionSnapshot learningSession =
        learningSessionService.findOwnedInProgressForUpdate(userId, sessionId);
    ScenarioSessionMessageContextProjection scenarioContext = findScenarioContext(sessionId);
    SessionHistoryLookup sessionHistoryLookup = findOrCreateSessionHistory(learningSession);
    SessionHistorySnapshot sessionHistory = sessionHistoryLookup.sessionHistory();
    List<SessionHistoryMessageSnapshot> previousMessages = findPreviousMessages(sessionHistory);

    SessionHistoryMessageSnapshot pending =
        previousMessages.isEmpty() ? null : previousMessages.getLast();
    if (pending != null && pending.getRole() == ConversationSpeaker.USER) {
      if (pending.getScenarioLeaseUntil() != null
          && java.time.LocalDateTime.now(clock).isBefore(pending.getScenarioLeaseUntil())) {
        throw new ApiException(ErrorCode.CONFLICT, "같은 발화의 다음 질문을 생성하고 있습니다.");
      }
      previousMessages = new ArrayList<>(previousMessages.subList(0, previousMessages.size() - 1));
      if (clientMessageId == null
          && pending.getClientMessageId() == null
          && (!pending.getContent().equals(content) || pending.getInputType() != inputType)) {
        // 구 FE의 재녹음은 종료된 시도만 교체한다.
        conversationMessageService.deleteIfExists(pending.getId());
        pending = null;
      } else {
        requireSameInput(pending, content, inputType, clientMessageId);
      }
    } else {
      pending = null;
    }
    int submittedTurnNumber =
        pending == null ? submittedTurnNumber(previousMessages) : pending.getTurnNumber();
    SessionHistoryMessageSnapshot submittedMessage =
        pending == null
            ? saveUserMessage(
                sessionHistory, previousMessages, submittedTurnNumber, content, inputType)
            : pending;
    String attempt =
        conversationMessageService.claimScenarioGeneration(
            submittedMessage.getId(),
            clientMessageId,
            java.time.LocalDateTime.now(clock).plus(aiProperties.requestTimeout()).plusSeconds(30));
    List<AiConversationHistoryMessage> conversationHistory =
        toConversationHistory(previousMessages, submittedMessage);

    Optional<NextQuestionContext> nextQuestion =
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
        sessionHistoryLookup.created(),
        attempt);
  }

  /** 키 있는 발화는 보존하고 구 FE의 키 없는 실패 발화만 기존 계약대로 정리한다. */
  void remove(SubmittedMessageContext submittedContext) {
    learningSessionService.findOwnedForUpdate(
        submittedContext.userId(), submittedContext.sessionId());
    var pending =
        conversationMessageService.findAll(submittedContext.sessionHistoryId()).stream()
            .filter(message -> message.getId().equals(submittedContext.submittedMessageId()))
            .findFirst()
            .orElse(null);
    if (pending == null
        || pending.getScenarioResponsePayload() != null
        || !java.util.Objects.equals(
            pending.getScenarioAttemptToken(), submittedContext.attemptToken())) {
      return;
    }
    if (pending.getClientMessageId() == null) {
      conversationMessageService.deleteIfExists(pending.getId());
    } else {
      conversationMessageService.releaseScenarioAttempt(
          pending.getId(), submittedContext.attemptToken());
    }
  }

  /** 완료된 같은 발화는 구독 변경과 관계없이 저장한 응답을 그대로 반환한다. */
  com.landit.landitbe.feature.session.scenario.message.dto.SessionMessageSubmitResponse replay(
      long userId,
      long sessionId,
      String content,
      SessionMessageInputType inputType,
      String clientMessageId) {
    learningSessionService.findOwnedForUpdate(userId, sessionId);
    if (clientMessageId == null) {
      return null;
    }
    var history = sessionHistoryService.findByLearningSessionId(sessionId);
    if (history.isEmpty()) {
      return null;
    }
    var stored =
        conversationMessageService.findAll(history.get().getId()).stream()
            .filter(message -> clientMessageId.equals(message.getClientMessageId()))
            .findFirst()
            .orElse(null);
    if (stored == null) {
      return null;
    }
    requireSameInput(stored, content, inputType, clientMessageId);
    return stored.getScenarioResponsePayload() == null
        ? null
        : mapper.readValue(
            stored.getScenarioResponsePayload(),
            com.landit.landitbe.feature.session.scenario.message.dto.SessionMessageSubmitResponse
                .class);
  }

  private void requireSameInput(
      SessionHistoryMessageSnapshot stored,
      String content,
      SessionMessageInputType inputType,
      String clientMessageId) {
    if (!stored.getContent().equals(content)
        || stored.getInputType() != inputType
        || !java.util.Objects.equals(stored.getClientMessageId(), clientMessageId)) {
      throw new ApiException(ErrorCode.CONFLICT, "같은 메시지 ID의 내용이나 입력 방식이 다릅니다.");
    }
  }

  private ScenarioSessionMessageContextProjection findScenarioContext(long sessionId) {
    return scenarioSessionService.requireMessageContext(sessionId);
  }

  private SessionHistoryLookup findOrCreateSessionHistory(LearningSessionSnapshot learningSession) {
    Optional<SessionHistorySnapshot> sessionHistory =
        sessionHistoryService.findByLearningSessionId(learningSession.getId());
    if (sessionHistory.isPresent()) {
      return new SessionHistoryLookup(sessionHistory.get(), false);
    }
    return new SessionHistoryLookup(
        sessionHistoryService.startScenario(
            learningSession.getId(),
            learningSession.getUserProfileId(),
            learningSession.getTargetLocale(),
            learningSession.getBaseLocale(),
            learningSession.getStartedAt()),
        true);
  }

  private List<SessionHistoryMessageSnapshot> findPreviousMessages(
      SessionHistorySnapshot sessionHistory) {
    return conversationMessageService.findAll(sessionHistory.getId());
  }

  /** 기존 히스토리 기준으로 이번 사용자 메시지가 답변할 턴 번호를 계산한다. */
  private int submittedTurnNumber(List<SessionHistoryMessageSnapshot> previousMessages) {
    if (previousMessages.isEmpty()) {
      return 1;
    }
    SessionHistoryMessageSnapshot lastMessage = previousMessages.get(previousMessages.size() - 1);
    if (lastMessage.getRole() == ConversationSpeaker.USER) {
      throw new ApiException(ErrorCode.CONFLICT, "처리 중인 사용자 메시지가 있습니다.");
    }
    return lastMessage.getTurnNumber();
  }

  private SessionHistoryMessageSnapshot saveUserMessage(
      SessionHistorySnapshot sessionHistory,
      List<SessionHistoryMessageSnapshot> previousMessages,
      int submittedTurnNumber,
      String content,
      SessionMessageInputType inputType) {
    SessionHistoryMessageSnapshot submittedMessage =
        conversationMessageService.recordUser(
            sessionHistory.getId(),
            previousMessages.size() + 1,
            submittedTurnNumber,
            content,
            inputType);
    return submittedMessage;
  }

  /** AI first는 시작 질문 다음 순서부터, USER first는 첫 질문부터 조회한다. */
  private int nextQuestionOrder(
      ScenarioSessionMessageContextProjection scenarioContext, int submittedTurnNumber) {
    return scenarioContext.firstSpeaker() == ConversationSpeaker.AI
        ? submittedTurnNumber + 1
        : submittedTurnNumber;
  }

  private Optional<NextQuestionContext> findNextQuestion(
      LearningSessionSnapshot learningSession,
      ScenarioSessionMessageContextProjection scenarioContext,
      int nextQuestionOrder) {
    return scenarioContentService.findActiveQuestion(
        scenarioContext.scenarioId(),
        nextQuestionOrder,
        scenarioContext.questionLevelGroup(),
        learningSession.getTargetLocale(),
        learningSession.getBaseLocale());
  }

  private List<AiConversationHistoryMessage> toConversationHistory(
      List<SessionHistoryMessageSnapshot> previousMessages,
      SessionHistoryMessageSnapshot submittedMessage) {
    List<SessionHistoryMessageSnapshot> messages = new ArrayList<>(previousMessages);
    messages.add(submittedMessage);
    return messages.stream().map(this::toConversationHistoryMessage).toList();
  }

  private AiConversationHistoryMessage toConversationHistoryMessage(
      SessionHistoryMessageSnapshot message) {
    return new AiConversationHistoryMessage(
        message.getId(),
        message.getTurnNumber(),
        message.getRole().name(),
        message.getContent(),
        message.getTranslatedContent());
  }

  private record SessionHistoryLookup(SessionHistorySnapshot sessionHistory, boolean created) {}
}
