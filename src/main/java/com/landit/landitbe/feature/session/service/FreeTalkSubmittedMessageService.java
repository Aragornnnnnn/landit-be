// 프리톡 발화의 짧은 트랜잭션 예약, 확정, 보상을 담당한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse.NextMessageResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse.ProgressResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse.SubmittedMessageResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 발화의 짧은 트랜잭션 예약, 확정, 보상을 담당한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkSubmittedMessageService {

  private static final long SPEAKING_TIME_LIMIT_MS = 60_000L;
  private static final long PROCESSING_TIMEOUT_SECONDS = 90;

  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService;

  /**
   * 같은 클라이언트 메시지 ID의 처리 완료 결과를 다시 구성한다.
   *
   * @param userId 요청 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @param request 재전송된 사용자 발화 요청
   * @return 저장된 처리 결과. 이전 발화가 없거나 아직 완료되지 않았으면 null
   * @throws ApiException 세션이 없거나 소유자가 다르거나 처리 상태가 충돌할 때
   */
  @Transactional
  public FreeTalkMessageSubmitResponse findCompletedResponse(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    // 요청 사용자의 세션과 저장된 사용자 메시지를 확인한다.
    requireOwnedSession(userId, learningSessionId);
    FreeTalkSession session = requireFreeTalkForUpdate(learningSessionId);
    clearExpiredProcessing(session);
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    SessionHistoryMessage userMessage =
        sessionHistoryMessageRepository
            .findBySessionHistoryIdAndClientMessageId(history.getId(), request.clientMessageId())
            .orElse(null);
    if (userMessage == null) {
      return null;
    }
    if (session.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    List<SessionHistoryMessage> messages =
        sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
            history.getId());
    int userMessageIndex = indexOfMessage(messages, userMessage.getId());
    FreeTalkTurnStatus storedTurnStatus = userMessage.getFreeTalkTurnStatus();
    if (storedTurnStatus == null) {
      return null;
    }
    if (storedTurnStatus == FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED) {
      return replayResponse(
          learningSessionId,
          session.getTitle(),
          FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED,
          userMessage,
          null,
          FreeTalkConversationStatus.AWAITING_EXIT_DECISION,
          speakingDurationUntil(messages, userMessage.getMessageSequence()),
          userId,
          session.getExpressionGenerationStatus());
    }

    // 저장된 다음 AI 메시지와 완료 당시의 대화 상태로 응답을 복원한다.
    SessionHistoryMessage nextMessage = requireNextAiMessage(messages, userMessageIndex);
    return replayResponse(
        learningSessionId,
        session.getTitle(),
        storedTurnStatus,
        userMessage,
        nextMessage,
        replayedConversationStatus(storedTurnStatus),
        speakingDurationUntil(messages, userMessage.getMessageSequence()),
        userId,
        session.getExpressionGenerationStatus());
  }

  /**
   * 종료 확인이 완료된 사용자 메시지의 처리 결과를 다시 구성한다.
   *
   * @param userId 요청 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @param submittedMessageId 종료 확인 대상 사용자 메시지 ID
   * @param decision 재전송된 종료 확인 결과
   * @return 저장된 처리 결과. 아직 종료 확인이 완료되지 않았으면 null
   * @throws ApiException 세션이 없거나 소유자가 다르거나 저장된 결정과 충돌할 때
   */
  @Transactional
  public FreeTalkMessageSubmitResponse findCompletedDecisionResponse(
      long userId, long learningSessionId, long submittedMessageId, FreeTalkExitDecision decision) {
    // 요청 사용자의 세션과 종료 확인 대상 메시지를 확인한다.
    requireOwnedSession(userId, learningSessionId);
    FreeTalkSession session = requireFreeTalkForUpdate(learningSessionId);
    clearExpiredProcessing(session);
    if (session.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    SessionHistoryMessage userMessage =
        sessionHistoryMessageRepository
            .findByIdAndSessionHistoryId(submittedMessageId, history.getId())
            .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT));
    FreeTalkTurnStatus storedTurnStatus = userMessage.getFreeTalkTurnStatus();
    if (storedTurnStatus != FreeTalkTurnStatus.CONTINUE
        && storedTurnStatus != FreeTalkTurnStatus.COMPLETED) {
      return null;
    }
    if ((storedTurnStatus == FreeTalkTurnStatus.CONTINUE
            && decision != FreeTalkExitDecision.CONTINUE)
        || (storedTurnStatus == FreeTalkTurnStatus.COMPLETED
            && decision != FreeTalkExitDecision.END)) {
      throw new ApiException(ErrorCode.CONFLICT);
    }

    // 저장된 다음 AI 메시지와 완료 당시의 대화 상태로 응답을 복원한다.
    List<SessionHistoryMessage> messages =
        sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
            history.getId());
    int userMessageIndex = indexOfMessage(messages, userMessage.getId());
    SessionHistoryMessage nextMessage = requireNextAiMessage(messages, userMessageIndex);
    return replayResponse(
        learningSessionId,
        session.getTitle(),
        storedTurnStatus,
        userMessage,
        nextMessage,
        replayedConversationStatus(storedTurnStatus),
        speakingDurationUntil(messages, userMessage.getMessageSequence()),
        userId,
        session.getExpressionGenerationStatus());
  }

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
  public Reservation reserve(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    final LearningSession learningSession = requireOwnedSession(userId, learningSessionId);
    FreeTalkSession freeTalkSession = requireFreeTalkForUpdate(learningSessionId);
    clearExpiredProcessing(freeTalkSession);
    SessionHistory history = requireHistory(learningSessionId);
    validateReservable(learningSession, freeTalkSession);
    List<SessionHistoryMessage> messages =
        sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
            history.getId());
    SessionHistoryMessage existingMessage =
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

  private SessionHistory requireHistory(long learningSessionId) {
    return sessionHistoryRepository
        .findByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  private void validateReservable(
      LearningSession learningSession, FreeTalkSession freeTalkSession) {
    if (!learningSession.isInProgress()
        || freeTalkSession.getConversationStatus() == FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
    }
    if (freeTalkSession.getConversationStatus() == FreeTalkConversationStatus.AWAITING_EXIT_DECISION
        || freeTalkSession.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
  }

  private Reservation reserveExistingMessage(
      long userId,
      FreeTalkMessageSubmitRequest request,
      LearningSession learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistory history,
      SessionHistoryMessage existingMessage,
      List<SessionHistoryMessage> messages) {
    if (existingMessage.getFreeTalkTurnStatus() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    freeTalkSession.startProcessing(request.clientMessageId());
    boolean shouldCloseAfterMessage = dailySpeakingUsageService.usage(userId).remainingMs() == 0;
    return reservation(
        learningSession,
        freeTalkSession,
        history,
        existingMessage,
        messages,
        shouldCloseAfterMessage);
  }

  private Reservation reserveNewMessage(
      long userId,
      FreeTalkMessageSubmitRequest request,
      LearningSession learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistory history,
      List<SessionHistoryMessage> messages) {
    if (messages.stream()
        .anyMatch(
            message ->
                message.getRole() == ConversationSpeaker.USER
                    && message.getFreeTalkTurnStatus() == null)) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    int userTurnNumber = nextUserTurnNumber(messages);
    FreeTalkDailySpeakingUsageService.DailySpeakingUsage dailyUsage =
        dailySpeakingUsageService.reserve(userId, request.utteranceDurationMs());
    SessionHistoryMessage userMessage =
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.freeTalkUser(
                history.getId(),
                messages.size() + 1,
                userTurnNumber,
                request.clientMessageId(),
                request.content(),
                request.inputType(),
                request.utteranceDurationMs()));
    freeTalkSession.startProcessing(request.clientMessageId());
    messages.add(userMessage);
    return reservation(
        learningSession,
        freeTalkSession,
        history,
        userMessage,
        messages,
        dailyUsage.remainingMs() == 0);
  }

  private Reservation reservation(
      LearningSession learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistory history,
      SessionHistoryMessage userMessage,
      List<SessionHistoryMessage> messages,
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
    return new Reservation(
        learningSession.getId(),
        freeTalkSession.getId(),
        history.getId(),
        userMessage.getId(),
        userMessage.getClientMessageId(),
        userMessage.getUtteranceDurationMs(),
        shouldCloseAfterMessage,
        freeTalkSession.getStartMode() == FreeTalkStartMode.USER_FIRST
            && messages.stream()
                    .filter(message -> message.getRole() == ConversationSpeaker.USER)
                    .count()
                == 1,
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
      Reservation reservation, AiFreeTalkTurnResult result) {
    ManagedRecords records = managedRecords(reservation);
    FreeTalkSession session = records.freeTalkSession();
    requireProcessingOwner(session, reservation.clientMessageId());
    SessionHistoryMessage userMessage = records.userMessage();
    session.addSpeakingDuration(reservation.utteranceDurationMs());
    if (result.inferredTitle() != null && session.getTitle() == null) {
      session.assignTitle(result.inferredTitle());
    }
    FreeTalkMessageSubmitResponse response;
    if (result.userExitIntentDetected()) {
      userMessage.recordFreeTalkTurnStatus(FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED);
      session.awaitExitDecision(userMessage.getId());
      session.clearProcessing();
      response =
          response(
              records.learningSessionId(),
              session,
              FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED,
              userMessage,
              null,
              records.learningSession().getUserProfileId());
    } else {
      userMessage.recordFreeTalkTurnStatus(FreeTalkTurnStatus.CONTINUE);
      userMessage.prepareInnerThought();
      SessionHistoryMessage aiMessage =
          sessionHistoryMessageRepository.save(
              SessionHistoryMessage.freeTalkAi(
                  records.history().getId(),
                  nextSequence(records.history().getId()),
                  userMessage.getTurnNumber() + 1,
                  result.aiMessage(),
                  result.translatedMessage(),
                  result.emotion()));
      session.clearProcessing();
      response =
          response(
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
      Reservation reservation, AiFreeTalkClosingResult result) {
    ManagedRecords records = managedRecords(reservation);
    FreeTalkSession session = records.freeTalkSession();
    requireProcessingOwner(session, reservation.clientMessageId());
    SessionHistoryMessage userMessage = records.userMessage();
    session.addSpeakingDuration(reservation.utteranceDurationMs());
    userMessage.recordFreeTalkTurnStatus(FreeTalkTurnStatus.COMPLETED);
    userMessage.prepareInnerThought();
    session.completeByTimeLimit();
    session.clearProcessing();
    records.learningSession().completeFreeTalkByTimeLimit(LocalDateTime.now());
    records
        .history()
        .complete(
            LocalDateTime.now(),
            Math.toIntExact(
                sessionHistoryMessageRepository.countBySessionHistoryIdAndRole(
                    records.history().getId(), ConversationSpeaker.USER)));
    SessionHistoryMessage aiMessage =
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.freeTalkAi(
                records.history().getId(),
                nextSequence(records.history().getId()),
                userMessage.getTurnNumber() + 1,
                result.aiMessage(),
                result.translatedMessage(),
                result.emotion()));
    return response(
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
  public void compensate(Reservation reservation) {
    FreeTalkSession session =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(reservation.learningSessionId())
            .orElse(null);
    if (session != null
        && reservation.clientMessageId().equals(session.getProcessingClientMessageId())) {
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
   */
  @Transactional
  public DecisionReservation reserveDecision(
      long userId, long learningSessionId, long submittedMessageId, FreeTalkExitDecision decision) {
    final LearningSession learningSession = requireOwnedSession(userId, learningSessionId);
    FreeTalkSession session = requireFreeTalkForUpdate(learningSessionId);
    clearExpiredProcessing(session);
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    SessionHistoryMessage userMessage =
        sessionHistoryMessageRepository
            .findByIdAndSessionHistoryId(submittedMessageId, history.getId())
            .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT));
    if (session.getConversationStatus() != FreeTalkConversationStatus.AWAITING_EXIT_DECISION
        || !Long.valueOf(submittedMessageId).equals(session.getPendingUserMessageId())
        || session.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    session.startProcessing("decision-" + submittedMessageId);
    AiFreeTalkTopic topic = new AiFreeTalkTopic(session.getTopicId(), session.getTitle(), null);
    return new DecisionReservation(
        learningSessionId,
        history.getId(),
        session.getId(),
        submittedMessageId,
        decision,
        learningSession.getTargetLocale().name(),
        learningSession.getBaseLocale().name(),
        topic,
        historyMessages(
            sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
                history.getId())));
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
      DecisionReservation reservation, AiFreeTalkTurnResult result) {
    ManagedRecords records =
        managedRecords(
            reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
    requireProcessingOwner(
        records.freeTalkSession(), decisionProcessingClientMessageId(reservation));
    records.userMessage().recordFreeTalkTurnStatus(FreeTalkTurnStatus.CONTINUE);
    records.userMessage().prepareInnerThought();
    if (result.inferredTitle() != null && records.freeTalkSession().getTitle() == null) {
      records.freeTalkSession().assignTitle(result.inferredTitle());
    }
    SessionHistoryMessage aiMessage =
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.freeTalkAi(
                records.history().getId(),
                nextSequence(records.history().getId()),
                records.userMessage().getTurnNumber() + 1,
                result.aiMessage(),
                result.translatedMessage(),
                result.emotion()));
    records.freeTalkSession().continueConversation();
    records.freeTalkSession().clearProcessing();
    return response(
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
      DecisionReservation reservation, AiFreeTalkClosingResult result) {
    ManagedRecords records =
        managedRecords(
            reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
    requireProcessingOwner(
        records.freeTalkSession(), decisionProcessingClientMessageId(reservation));
    records.userMessage().recordFreeTalkTurnStatus(FreeTalkTurnStatus.COMPLETED);
    records.userMessage().prepareInnerThought();
    final SessionHistoryMessage aiMessage =
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.freeTalkAi(
                records.history().getId(),
                nextSequence(records.history().getId()),
                records.userMessage().getTurnNumber() + 1,
                result.aiMessage(),
                result.translatedMessage(),
                result.emotion()));
    records.freeTalkSession().completeByUserExit();
    records.freeTalkSession().clearProcessing();
    records.learningSession().completeFreeTalkByUser(LocalDateTime.now());
    records
        .history()
        .complete(
            LocalDateTime.now(),
            Math.toIntExact(
                sessionHistoryMessageRepository.countBySessionHistoryIdAndRole(
                    records.history().getId(), ConversationSpeaker.USER)));
    return response(
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
  public void compensateDecision(DecisionReservation reservation) {
    freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(reservation.learningSessionId())
        .filter(
            session ->
                decisionProcessingClientMessageId(reservation)
                    .equals(session.getProcessingClientMessageId()))
        .ifPresent(FreeTalkSession::clearProcessing);
  }

  private SessionHistoryMessage requireNextAiMessage(
      List<SessionHistoryMessage> messages, int userMessageIndex) {
    if (userMessageIndex + 1 >= messages.size()) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    SessionHistoryMessage nextMessage = messages.get(userMessageIndex + 1);
    if (nextMessage.getRole() != ConversationSpeaker.AI) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    return nextMessage;
  }

  private FreeTalkConversationStatus replayedConversationStatus(
      FreeTalkTurnStatus storedTurnStatus) {
    return storedTurnStatus == FreeTalkTurnStatus.COMPLETED
        ? FreeTalkConversationStatus.COMPLETED
        : FreeTalkConversationStatus.IN_PROGRESS;
  }

  private ManagedRecords managedRecords(Reservation reservation) {
    return managedRecords(
        reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
  }

  private ManagedRecords managedRecords(
      long learningSessionId, long historyId, long userMessageId) {
    LearningSession learningSession = requireOwnedSessionWithoutUser(learningSessionId);
    FreeTalkSession session = requireFreeTalkForUpdate(learningSessionId);
    SessionHistory history =
        sessionHistoryRepository
            .findById(historyId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    SessionHistoryMessage userMessage =
        sessionHistoryMessageRepository
            .findById(userMessageId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    return new ManagedRecords(learningSessionId, learningSession, session, history, userMessage);
  }

  private LearningSession requireOwnedSession(long userId, long learningSessionId) {
    LearningSession session =
        learningSessionRepository
            .findById(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    if (!Long.valueOf(userId).equals(session.getUserProfileId())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    return learningSessionRepository
        .findByIdAndUserProfileIdForUpdate(learningSessionId, userId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  private LearningSession requireOwnedSessionWithoutUser(long learningSessionId) {
    return learningSessionRepository
        .findById(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  private FreeTalkSession requireFreeTalkForUpdate(long learningSessionId) {
    return freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  private void requireProcessingOwner(FreeTalkSession session, String processingClientMessageId) {
    if (!processingClientMessageId.equals(session.getProcessingClientMessageId())) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
  }

  private String decisionProcessingClientMessageId(DecisionReservation reservation) {
    return "decision-" + reservation.userMessageId();
  }

  private void clearExpiredProcessing(FreeTalkSession session) {
    session.clearProcessingIfExpired(LocalDateTime.now().minusSeconds(PROCESSING_TIMEOUT_SECONDS));
  }

  private int nextUserTurnNumber(List<SessionHistoryMessage> messages) {
    if (messages.isEmpty()) {
      return 1;
    }
    SessionHistoryMessage latest = messages.getLast();
    return latest.getRole() == ConversationSpeaker.AI
        ? latest.getTurnNumber()
        : latest.getTurnNumber() + 1;
  }

  private int indexOfMessage(List<SessionHistoryMessage> messages, long messageId) {
    for (int index = 0; index < messages.size(); index++) {
      if (Long.valueOf(messageId).equals(messages.get(index).getId())) {
        return index;
      }
    }
    throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
  }

  private long speakingDurationUntil(List<SessionHistoryMessage> messages, int messageSequence) {
    return messages.stream()
        .filter(message -> message.getRole() == ConversationSpeaker.USER)
        .filter(message -> message.getMessageSequence() <= messageSequence)
        .map(SessionHistoryMessage::getUtteranceDurationMs)
        .filter(duration -> duration != null)
        .mapToLong(Long::longValue)
        .sum();
  }

  private int nextSequence(long historyId) {
    return sessionHistoryMessageRepository
            .findBySessionHistoryIdOrderByMessageSequenceAsc(historyId)
            .size()
        + 1;
  }

  private List<AiConversationHistoryMessage> historyMessages(List<SessionHistoryMessage> messages) {
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

  private FreeTalkMessageSubmitResponse response(
      long learningSessionId,
      FreeTalkSession session,
      FreeTalkTurnStatus turnStatus,
      SessionHistoryMessage userMessage,
      SessionHistoryMessage aiMessage,
      long userId) {
    FreeTalkDailySpeakingUsageService.DailySpeakingUsage dailyUsage =
        dailySpeakingUsageService.usage(userId);
    return new FreeTalkMessageSubmitResponse(
        learningSessionId,
        session.getTitle(),
        turnStatus,
        SubmittedMessageResponse.from(userMessage),
        aiMessage == null ? null : NextMessageResponse.from(aiMessage),
        new ProgressResponse(
            session.getConversationStatus(),
            session.getAccumulatedSpeakingDurationMs(),
            SPEAKING_TIME_LIMIT_MS,
            dailyUsage.usedSpeakingDurationMs(),
            dailyUsage.remainingMs(),
            session.getExpressionGenerationStatus()));
  }

  private FreeTalkMessageSubmitResponse replayResponse(
      long learningSessionId,
      String title,
      FreeTalkTurnStatus turnStatus,
      SessionHistoryMessage userMessage,
      SessionHistoryMessage aiMessage,
      FreeTalkConversationStatus conversationStatus,
      long accumulatedSpeakingDurationMs,
      long userId,
      ExpressionGenerationStatus expressionGenerationStatus) {
    FreeTalkDailySpeakingUsageService.DailySpeakingUsage dailyUsage =
        dailySpeakingUsageService.usage(userId);
    return new FreeTalkMessageSubmitResponse(
        learningSessionId,
        title,
        turnStatus,
        SubmittedMessageResponse.from(userMessage),
        aiMessage == null ? null : NextMessageResponse.from(aiMessage),
        new ProgressResponse(
            conversationStatus,
            accumulatedSpeakingDurationMs,
            SPEAKING_TIME_LIMIT_MS,
            dailyUsage.usedSpeakingDurationMs(),
            dailyUsage.remainingMs(),
            expressionGenerationStatus));
  }

  /**
   * AI 호출 전에 저장한 사용자 발화와 대화 문맥이다.
   *
   * @param learningSessionId 학습 세션 ID
   * @param freeTalkSessionId 프리톡 세션 ID
   * @param historyId 세션 히스토리 ID
   * @param userMessageId 저장된 사용자 메시지 ID
   * @param clientMessageId 중복 요청을 식별하는 클라이언트 메시지 ID
   * @param utteranceDurationMs 이번 사용자 발화 시간 밀리초
   * @param dailyLimitReached 이번 발화 예약 후 일일 한도에 도달했는지 여부
   * @param firstUserTurn 사용자 선시작 세션의 첫 사용자 턴인지 여부
   * @param targetLocale 학습 대상 언어
   * @param baseLocale 사용자 기준 언어
   * @param topic 프리톡 주제
   * @param history AI에 전달할 대화 기록
   */
  public record Reservation(
      long learningSessionId,
      long freeTalkSessionId,
      long historyId,
      long userMessageId,
      String clientMessageId,
      long utteranceDurationMs,
      boolean dailyLimitReached,
      boolean firstUserTurn,
      String targetLocale,
      String baseLocale,
      AiFreeTalkTopic topic,
      List<AiConversationHistoryMessage> history) {}

  /**
   * 종료 확인 전에 저장한 사용자 메시지와 대화 문맥이다.
   *
   * @param learningSessionId 학습 세션 ID
   * @param historyId 세션 히스토리 ID
   * @param freeTalkSessionId 프리톡 세션 ID
   * @param userMessageId 종료 의사가 감지된 사용자 메시지 ID
   * @param decision 사용자가 선택한 종료 여부
   * @param targetLocale 학습 대상 언어
   * @param baseLocale 사용자 기준 언어
   * @param topic 프리톡 주제
   * @param history AI에 전달할 대화 기록
   */
  public record DecisionReservation(
      long learningSessionId,
      long historyId,
      long freeTalkSessionId,
      long userMessageId,
      FreeTalkExitDecision decision,
      String targetLocale,
      String baseLocale,
      AiFreeTalkTopic topic,
      List<AiConversationHistoryMessage> history) {}

  private record ManagedRecords(
      long learningSessionId,
      LearningSession learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistory history,
      SessionHistoryMessage userMessage) {}
}
