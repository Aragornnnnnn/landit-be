// 프리톡 발화의 짧은 트랜잭션 예약, 확정, 보상을 담당한다.

package com.landit.landitbe.feature.learning.freetalk.message.service;

import com.landit.landitbe.config.memory.MemoryProperties;
import com.landit.landitbe.feature.character.service.StreakService;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkCharacter;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkExitDecisionReservation;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageReservation;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.usage.dto.DailySpeakingUsage;
import com.landit.landitbe.feature.learning.freetalk.usage.service.FreeTalkDailySpeakingUsageService;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 발화의 짧은 트랜잭션 예약, 확정, 보상을 담당한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkSubmittedMessageService {
  private final FreeTalkMessageSessionService sessionService;
  private final FreeTalkMessageResponseService responseService;

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final UserProfileService userProfileService;
  private final LearningSessionService learningSessionService;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final SessionHistoryService sessionHistoryService;
  private final ConversationMessageService conversationMessageService;
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService;
  private final StreakService streakService;
  private final MemoryProperties memoryProperties;
  private final Clock clock;
  private final com.landit.landitbe.feature.subscription.service.LearningAccessGrantService
      accessGrants;

  /**
   * 사용자 발화를 저장하고 외부 AI 호출에 필요한 예약 정보를 만든다.
   *
   * @param userId 요청 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @param request 사용자 발화 요청
   * @return 외부 AI 호출과 후속 확정에 사용할 예약 정보
   * @throws ApiException 세션이 없거나 소유자가 다르거나 완료·처리 중 상태일 때
   */
  @Transactional
  public FreeTalkMessageReservation reserve(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    final LearningSessionSnapshot learningSession =
        sessionService.requireOwnedSession(userId, learningSessionId);
    FreeTalkSession freeTalkSession = sessionService.requireFreeTalkForUpdate(learningSessionId);
    sessionService.clearExpiredProcessing(freeTalkSession);
    SessionHistorySnapshot history = requireHistory(learningSessionId);
    validateReservable(learningSession, freeTalkSession);
    List<SessionHistoryMessageSnapshot> messages =
        conversationMessageService.findAll(history.getId());
    SessionHistoryMessageSnapshot existingMessage =
        messages.stream()
            .filter(message -> request.clientMessageId().equals(message.getClientMessageId()))
            .findFirst()
            .orElse(null);
    if (existingMessage != null) {
      return reserveExistingMessage(
          userId, request, learningSession, freeTalkSession, history, existingMessage, messages);
    }
    return reserveNewMessage(userId, request, learningSession, freeTalkSession, history, messages);
  }

  private SessionHistorySnapshot requireHistory(long learningSessionId) {
    return sessionHistoryService
        .findByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  private void validateReservable(
      LearningSessionSnapshot learningSession, FreeTalkSession freeTalkSession) {
    if (!learningSession.isInProgress()
        || freeTalkSession.getConversationStatus() == FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(SessionErrorCode.SESSION_ALREADY_COMPLETED);
    }
    if (freeTalkSession.getConversationStatus() == FreeTalkConversationStatus.AWAITING_EXIT_DECISION
        || freeTalkSession.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
  }

  private FreeTalkMessageReservation reserveExistingMessage(
      long userId,
      FreeTalkMessageSubmitRequest request,
      LearningSessionSnapshot learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistorySnapshot history,
      SessionHistoryMessageSnapshot existingMessage,
      List<SessionHistoryMessageSnapshot> messages) {
    if (existingMessage.getFreeTalkTurnStatus() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    DailySpeakingUsage dailyUsage =
        dailySpeakingUsageService.reserve(userId, existingMessage.getUtteranceDurationMs());
    freeTalkSession.startProcessing(request.clientMessageId());
    return reservation(
        userId,
        dailyUsage.usageDate(),
        learningSession,
        freeTalkSession,
        history,
        existingMessage,
        messages,
        dailyUsage.remainingMs() == 0);
  }

  private FreeTalkMessageReservation reserveNewMessage(
      long userId,
      FreeTalkMessageSubmitRequest request,
      LearningSessionSnapshot learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistorySnapshot history,
      List<SessionHistoryMessageSnapshot> messages) {
    if (messages.stream()
        .anyMatch(
            message ->
                message.getRole() == ConversationSpeaker.USER
                    && message.getFreeTalkTurnStatus() == null)) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    accessGrants.requireSessionContinuation(
        userId, "FREE_TALK", learningSession.getId(), learningSession.getStartedAt());
    int userTurnNumber = nextUserTurnNumber(messages);
    DailySpeakingUsage dailyUsage =
        dailySpeakingUsageService.reserve(userId, request.utteranceDurationMs());
    SessionHistoryMessageSnapshot userMessage =
        conversationMessageService.recordFreeTalkUser(
            history.getId(),
            messages.size() + 1,
            userTurnNumber,
            request.clientMessageId(),
            request.content(),
            request.inputType(),
            request.utteranceDurationMs());
    freeTalkSession.startProcessing(request.clientMessageId());
    messages.add(userMessage);
    return reservation(
        userId,
        dailyUsage.usageDate(),
        learningSession,
        freeTalkSession,
        history,
        userMessage,
        messages,
        dailyUsage.remainingMs() == 0);
  }

  private FreeTalkMessageReservation reservation(
      long userId,
      LocalDate usageDate,
      LearningSessionSnapshot learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistorySnapshot history,
      SessionHistoryMessageSnapshot userMessage,
      List<SessionHistoryMessageSnapshot> messages,
      boolean shouldCloseAfterMessage) {
    AiFreeTalkTopic topic =
        freeTalkSession.getTopicId() == null
            ? new AiFreeTalkTopic(null, freeTalkSession.getTitle(), null)
            : freeTalkTopicRepository
                .findById(freeTalkSession.getTopicId())
                .map(
                    topicValue ->
                        new AiFreeTalkTopic(
                            topicValue.getId(),
                            topicValue.getDisplayName(),
                            topicValue.getPromptDescription()))
                .orElse(new AiFreeTalkTopic(null, freeTalkSession.getTitle(), null));
    return new FreeTalkMessageReservation(
        userId,
        usageDate,
        learningSession.getId(),
        freeTalkSession.getId(),
        freeTalkSession.getCharacterId(),
        history.getId(),
        userMessage.getId(),
        userMessage.getClientMessageId(),
        userMessage.getUtteranceDurationMs(),
        shouldCloseAfterMessage,
        requiresTitleGeneration(freeTalkSession),
        learningSession.getTargetLocale().name(),
        learningSession.getBaseLocale().name(),
        topic,
        historyMessages(messages));
  }

  /**
   * 일반 AI 턴 또는 종료 의사 감지 결과를 저장한다.
   *
   * @param reservation 사용자 발화 저장 단계에서 만든 예약 정보
   * @param result AI가 생성한 후속 응답과 종료 의사 감지 결과
   * @return 저장된 사용자·AI 메시지와 현재 진행 상태
   * @throws ApiException 예약한 요청과 현재 처리 중인 요청이 다를 때
   */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeTurn(
      FreeTalkMessageReservation reservation, AiFreeTalkTurnResult result) {
    ManagedRecords records = managedRecords(reservation);
    FreeTalkSession session = records.freeTalkSession();
    requireProcessingOwner(session, reservation.clientMessageId());
    SessionHistoryMessageSnapshot userMessage = records.userMessage();
    session.addSpeakingDuration(reservation.utteranceDurationMs());
    FreeTalkMessageSubmitResponse response;
    if (result.userExitIntentDetected()) {
      userMessage =
          conversationMessageService.recordFreeTalkTurnResult(
              userMessage.getId(), FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED, false);
      session.awaitExitDecision(userMessage.getId());
      session.clearProcessing();
      response =
          responseService.buildResponse(
              records.learningSessionId(),
              session,
              FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED,
              userMessage,
              null,
              records.learningSession().getUserProfileId());
    } else {
      userMessage =
          conversationMessageService.recordFreeTalkTurnResult(
              userMessage.getId(), FreeTalkTurnStatus.CONTINUE, true);
      SessionHistoryMessageSnapshot aiMessage =
          conversationMessageService.recordFreeTalkAi(
              records.history().getId(),
              nextSequence(records.history().getId()),
              userMessage.getTurnNumber() + 1,
              result.aiMessage(),
              result.translatedMessage(),
              result.emotion());
      session.clearProcessing();
      response =
          responseService.buildResponse(
              records.learningSessionId(),
              session,
              FreeTalkTurnStatus.CONTINUE,
              userMessage,
              aiMessage,
              records.learningSession().getUserProfileId());
    }
    return response;
  }

  /**
   * 시간 제한 마무리 결과를 저장하고 세션을 완료한다.
   *
   * @param reservation 사용자 발화 저장 단계에서 만든 예약 정보
   * @param result AI가 생성한 마무리 응답
   * @return 저장된 사용자·AI 메시지와 완료 상태
   * @throws ApiException 예약한 요청과 현재 처리 중인 요청이 다를 때
   */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeTimeLimit(
      FreeTalkMessageReservation reservation, AiFreeTalkClosingResult result) {
    userProfileService.requireActiveForUpdate(reservation.userId());
    ManagedRecords records = managedRecords(reservation);
    FreeTalkSession session = records.freeTalkSession();
    requireProcessingOwner(session, reservation.clientMessageId());
    SessionHistoryMessageSnapshot userMessage = records.userMessage();
    session.addSpeakingDuration(reservation.utteranceDurationMs());
    assignClosingTitle(session, result, reservation.titleGenerationRequired());
    userMessage =
        conversationMessageService.recordFreeTalkTurnResult(
            userMessage.getId(), FreeTalkTurnStatus.COMPLETED, true);
    session.completeByTimeLimit();
    prepareMemoryGeneration(session);
    session.clearProcessing();
    LocalDateTime completedAt = LocalDateTime.ofInstant(clock.instant(), KOREA_ZONE_ID);
    learningSessionService.completeFreeTalkByTimeLimit(
        records.learningSession().getId(), completedAt);
    sessionHistoryService.complete(
        records.history().getId(),
        completedAt,
        Math.toIntExact(
            conversationMessageService.countByRole(
                records.history().getId(), ConversationSpeaker.USER)));
    streakService.recordCompletedConversation(
        records.learningSession().getUserProfileId(), completedAt);
    SessionHistoryMessageSnapshot aiMessage =
        conversationMessageService.recordFreeTalkAi(
            records.history().getId(),
            nextSequence(records.history().getId()),
            userMessage.getTurnNumber() + 1,
            result.aiMessage(),
            result.translatedMessage(),
            result.emotion());
    return responseService.buildResponse(
        records.learningSessionId(),
        session,
        FreeTalkTurnStatus.COMPLETED,
        userMessage,
        aiMessage,
        records.learningSession().getUserProfileId());
  }

  /**
   * AI 호출 실패 시 처리 표시만 되돌리고 사용자 발화 시간은 보존한다.
   *
   * @param reservation 실패한 AI 호출의 예약 정보
   */
  @Transactional
  public void compensate(FreeTalkMessageReservation reservation) {
    FreeTalkSession session =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(reservation.learningSessionId())
            .orElse(null);
    if (session != null
        && reservation.clientMessageId().equals(session.getProcessingClientMessageId())) {
      dailySpeakingUsageService.release(
          reservation.userId(), reservation.usageDate(), reservation.utteranceDurationMs());
      session.clearProcessing();
    }
  }

  /**
   * 종료 확인 처리에 필요한 기존 예약과 세션 상태를 잠금 조회한다.
   *
   * @param userId 요청 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @param submittedMessageId 종료 확인 대상 사용자 메시지 ID
   * @param decision 사용자가 선택한 종료 확인 결과
   * @return 외부 AI 호출과 후속 확정에 사용할 종료 결정 예약 정보
   * @throws ApiException 세션이 없거나 소유자가 다르거나 종료 확인 상태가 유효하지 않을 때
   * @throws com.landit.landitbe.feature.learning.conversation.exception.SessionException 프리톡 이용 한도에
   *     도달했을 때
   */
  @Transactional
  public FreeTalkExitDecisionReservation reserveDecision(
      long userId, long learningSessionId, long submittedMessageId, FreeTalkExitDecision decision) {
    final LearningSessionSnapshot learningSession =
        sessionService.requireOwnedSession(userId, learningSessionId);
    FreeTalkSession session = sessionService.requireFreeTalkForUpdate(learningSessionId);
    sessionService.clearExpiredProcessing(session);
    SessionHistorySnapshot history =
        sessionHistoryService
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    SessionHistoryMessageSnapshot userMessage =
        conversationMessageService
            .findMessageInHistory(submittedMessageId, history.getId())
            .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT));
    if (session.getConversationStatus() != FreeTalkConversationStatus.AWAITING_EXIT_DECISION
        || !Long.valueOf(submittedMessageId).equals(session.getPendingUserMessageId())
        || session.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    dailySpeakingUsageService.reserveRequest(userId);
    session.startProcessing("decision-" + submittedMessageId);
    AiFreeTalkTopic topic = new AiFreeTalkTopic(session.getTopicId(), session.getTitle(), null);
    return new FreeTalkExitDecisionReservation(
        userId,
        learningSessionId,
        history.getId(),
        session.getId(),
        session.getCharacterId(),
        submittedMessageId,
        decision,
        requiresTitleGeneration(session),
        learningSession.getTargetLocale().name(),
        learningSession.getBaseLocale().name(),
        topic,
        historyMessages(conversationMessageService.findAll(history.getId())));
  }

  /**
   * 종료 확인을 취소한 AI 턴을 저장한다.
   *
   * @param reservation 종료 확인 단계에서 만든 예약 정보
   * @param result AI가 생성한 후속 응답
   * @return 저장된 사용자·AI 메시지와 계속 상태
   * @throws ApiException 예약한 요청과 현재 처리 중인 요청이 다를 때
   */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeContinue(
      FreeTalkExitDecisionReservation reservation, AiFreeTalkTurnResult result) {
    ManagedRecords records =
        managedRecords(
            reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
    requireProcessingOwner(
        records.freeTalkSession(), decisionProcessingClientMessageId(reservation));
    records =
        records.withUserMessage(
            conversationMessageService.recordFreeTalkTurnResult(
                records.userMessage().getId(), FreeTalkTurnStatus.CONTINUE, true));
    SessionHistoryMessageSnapshot aiMessage =
        conversationMessageService.recordFreeTalkAi(
            records.history().getId(),
            nextSequence(records.history().getId()),
            records.userMessage().getTurnNumber() + 1,
            result.aiMessage(),
            result.translatedMessage(),
            result.emotion());
    records.freeTalkSession().continueConversation();
    records.freeTalkSession().clearProcessing();
    return responseService.buildResponse(
        records.learningSessionId(),
        records.freeTalkSession(),
        FreeTalkTurnStatus.CONTINUE,
        records.userMessage(),
        aiMessage,
        records.learningSession().getUserProfileId());
  }

  /**
   * 종료 확정의 AI 마무리 메시지를 저장하고 세션을 완료한다.
   *
   * @param reservation 종료 확인 단계에서 만든 예약 정보
   * @param result AI가 생성한 마무리 응답
   * @return 저장된 사용자·AI 메시지와 완료 상태
   * @throws ApiException 예약한 요청과 현재 처리 중인 요청이 다를 때
   */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeEnd(
      FreeTalkExitDecisionReservation reservation, AiFreeTalkClosingResult result) {
    userProfileService.requireActiveForUpdate(reservation.userId());
    ManagedRecords records =
        managedRecords(
            reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
    requireProcessingOwner(
        records.freeTalkSession(), decisionProcessingClientMessageId(reservation));
    assignClosingTitle(records.freeTalkSession(), result, reservation.titleGenerationRequired());
    records =
        records.withUserMessage(
            conversationMessageService.recordFreeTalkTurnResult(
                records.userMessage().getId(), FreeTalkTurnStatus.COMPLETED, true));
    final SessionHistoryMessageSnapshot aiMessage =
        conversationMessageService.recordFreeTalkAi(
            records.history().getId(),
            nextSequence(records.history().getId()),
            records.userMessage().getTurnNumber() + 1,
            result.aiMessage(),
            result.translatedMessage(),
            result.emotion());
    records.freeTalkSession().completeByUserExit();
    prepareMemoryGeneration(records.freeTalkSession());
    records.freeTalkSession().clearProcessing();
    LocalDateTime completedAt = LocalDateTime.ofInstant(clock.instant(), KOREA_ZONE_ID);
    learningSessionService.completeFreeTalkByUser(records.learningSession().getId(), completedAt);
    sessionHistoryService.complete(
        records.history().getId(),
        completedAt,
        Math.toIntExact(
            conversationMessageService.countByRole(
                records.history().getId(), ConversationSpeaker.USER)));
    streakService.recordCompletedConversation(
        records.learningSession().getUserProfileId(), completedAt);
    return responseService.buildResponse(
        records.learningSessionId(),
        records.freeTalkSession(),
        FreeTalkTurnStatus.COMPLETED,
        records.userMessage(),
        aiMessage,
        records.learningSession().getUserProfileId());
  }

  /**
   * 종료 확인의 AI 호출 실패 뒤 처리 표시만 되돌린다.
   *
   * @param reservation 실패한 종료 확인 AI 호출의 예약 정보
   */
  @Transactional
  public void compensateDecision(FreeTalkExitDecisionReservation reservation) {
    freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(reservation.learningSessionId())
        .filter(
            session ->
                decisionProcessingClientMessageId(reservation)
                    .equals(session.getProcessingClientMessageId()))
        .ifPresent(FreeTalkSession::clearProcessing);
  }

  private ManagedRecords managedRecords(FreeTalkMessageReservation reservation) {
    return managedRecords(
        reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
  }

  private ManagedRecords managedRecords(
      long learningSessionId, long historyId, long userMessageId) {
    LearningSessionSnapshot learningSession = requireOwnedSessionWithoutUser(learningSessionId);
    FreeTalkSession session = sessionService.requireFreeTalkForUpdate(learningSessionId);
    SessionHistorySnapshot history =
        sessionHistoryService
            .findHistory(historyId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    SessionHistoryMessageSnapshot userMessage =
        conversationMessageService
            .findMessage(userMessageId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    return new ManagedRecords(learningSessionId, learningSession, session, history, userMessage);
  }

  private LearningSessionSnapshot requireOwnedSessionWithoutUser(long learningSessionId) {
    return learningSessionService
        .findSession(learningSessionId)
        .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  private void requireProcessingOwner(FreeTalkSession session, String processingClientMessageId) {
    if (!processingClientMessageId.equals(session.getProcessingClientMessageId())) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
  }

  private boolean requiresTitleGeneration(FreeTalkSession session) {
    return session.getStartMode() == FreeTalkStartMode.USER_FIRST && session.getTitle() == null;
  }

  private void assignClosingTitle(
      FreeTalkSession session, AiFreeTalkClosingResult result, boolean titleGenerationRequired) {
    if (!titleGenerationRequired || session.getTitle() != null) {
      return;
    }
    String generatedTitle = result.inferredTitle();
    if (generatedTitle != null && !generatedTitle.isBlank()) {
      session.assignTitle(generatedTitle.strip());
      return;
    }
    String characterDisplayName = FreeTalkCharacter.fromId(session.getCharacterId()).displayName();
    session.assignTitle(characterDisplayName + "와의 대화");
  }

  private String decisionProcessingClientMessageId(FreeTalkExitDecisionReservation reservation) {
    return "decision-" + reservation.userMessageId();
  }

  private void prepareMemoryGeneration(FreeTalkSession session) {
    if (memoryProperties.writeEnabled()) {
      session.prepareMemoryGeneration();
    }
  }

  private int nextUserTurnNumber(List<SessionHistoryMessageSnapshot> messages) {
    if (messages.isEmpty()) {
      return 1;
    }
    SessionHistoryMessageSnapshot latest = messages.getLast();
    return latest.getRole() == ConversationSpeaker.AI
        ? latest.getTurnNumber()
        : latest.getTurnNumber() + 1;
  }

  private int nextSequence(long historyId) {
    return conversationMessageService.findAll(historyId).size() + 1;
  }

  private List<AiConversationHistoryMessage> historyMessages(
      List<SessionHistoryMessageSnapshot> messages) {
    return messages.stream()
        .map(
            message ->
                new AiConversationHistoryMessage(
                    message.getId(),
                    message.getTurnNumber(),
                    message.getRole().name(),
                    message.getContent(),
                    message.getTranslatedContent()))
        .toList();
  }

  private record ManagedRecords(
      long learningSessionId,
      LearningSessionSnapshot learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistorySnapshot history,
      SessionHistoryMessageSnapshot userMessage) {
    ManagedRecords withUserMessage(SessionHistoryMessageSnapshot message) {
      return new ManagedRecords(
          learningSessionId, learningSession, freeTalkSession, history, message);
    }
  }
}
