// 프리톡 발화의 짧은 트랜잭션 예약, 확정, 보상을 담당한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.service.AiTutorService;
import com.landit.landitbe.feature.content.service.AiTutorService.FreeTalkPartner;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnResult;
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

  private static final long SPEAKING_TIME_LIMIT_MS = 180_000L;

  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final AiTutorService aiTutorService;

  /** 사용자 발화를 저장하고 외부 AI 호출에 필요한 예약 정보를 만든다. */
  @Transactional
  public Reservation reserve(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    final LearningSession learningSession = requireOwnedSession(userId, learningSessionId);
    FreeTalkSession freeTalkSession = requireFreeTalkForUpdate(learningSessionId);
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    if (sessionHistoryMessageRepository.existsBySessionHistoryIdAndClientMessageId(
        history.getId(), request.clientMessageId())) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    if (!learningSession.isInProgress()
        || freeTalkSession.getConversationStatus() == FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
    }
    if (freeTalkSession.getConversationStatus() == FreeTalkConversationStatus.AWAITING_EXIT_DECISION
        || freeTalkSession.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }

    List<SessionHistoryMessage> messages =
        sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
            history.getId());
    int userTurnNumber = nextUserTurnNumber(messages);
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
    FreeTalkPartner partner =
        aiTutorService.requireFreeTalkPartner(
            learningSession.getAiTutorId(), learningSession.getBaseLocale());
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
        learningSessionId,
        freeTalkSession.getId(),
        history.getId(),
        userMessage.getId(),
        request.clientMessageId(),
        request.utteranceDurationMs(),
        request.timeLimitReached(),
        freeTalkSession.getStartMode() == FreeTalkStartMode.USER_FIRST
            && messages.stream()
                    .filter(message -> message.getRole() == ConversationSpeaker.USER)
                    .count()
                == 1,
        learningSession.getTargetLocale().name(),
        learningSession.getBaseLocale().name(),
        partner,
        topic,
        historyMessages(messages));
  }

  /** 일반 AI 턴 또는 종료 의사 감지 결과를 저장한다. */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeTurn(
      Reservation reservation, AiFreeTalkTurnResult result) {
    ManagedRecords records = managedRecords(reservation);
    FreeTalkSession session = records.freeTalkSession();
    SessionHistoryMessage userMessage = records.userMessage();
    session.addSpeakingDuration(reservation.utteranceDurationMs());
    if (result.inferredTitle() != null && session.getTitle() == null) {
      session.assignTitle(result.inferredTitle());
    }
    FreeTalkMessageSubmitResponse response;
    if (result.userExitIntentDetected()) {
      session.awaitExitDecision(userMessage.getId());
      session.clearProcessing();
      response =
          response(
              records.learningSessionId(),
              session,
              FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED,
              userMessage,
              null);
    } else {
      userMessage.recordInnerThought(result.innerThought(), result.innerThoughtType());
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
              aiMessage);
    }
    return response;
  }

  /** 시간 제한 마무리 결과를 저장하고 세션을 완료한다. */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeTimeLimit(
      Reservation reservation, AiFreeTalkClosingResult result) {
    ManagedRecords records = managedRecords(reservation);
    FreeTalkSession session = records.freeTalkSession();
    SessionHistoryMessage userMessage = records.userMessage();
    session.addSpeakingDuration(reservation.utteranceDurationMs());
    userMessage.recordInnerThought(result.innerThought(), result.innerThoughtType());
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
        records.learningSessionId(), session, FreeTalkTurnStatus.COMPLETED, userMessage, aiMessage);
  }

  /** AI 호출 실패 시 이번 요청에서 예약한 메시지와 처리 표시를 되돌린다. */
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
    sessionHistoryMessageRepository.deleteById(reservation.userMessageId());
  }

  /** 종료 확인 처리에 필요한 기존 예약과 세션 상태를 잠금 조회한다. */
  @Transactional
  public DecisionReservation reserveDecision(
      long userId, long learningSessionId, long submittedMessageId, FreeTalkExitDecision decision) {
    FreeTalkSession session = requireFreeTalkForUpdate(learningSessionId);
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    SessionHistoryMessage userMessage =
        sessionHistoryMessageRepository
            .findByIdAndSessionHistoryId(submittedMessageId, history.getId())
            .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT));
    final LearningSession learningSession = requireOwnedSession(userId, learningSessionId);
    if (session.getConversationStatus() != FreeTalkConversationStatus.AWAITING_EXIT_DECISION
        || !Long.valueOf(submittedMessageId).equals(session.getPendingUserMessageId())
        || session.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    session.startProcessing("decision-" + submittedMessageId);
    FreeTalkPartner partner =
        aiTutorService.requireFreeTalkPartner(
            learningSession.getAiTutorId(), learningSession.getBaseLocale());
    AiFreeTalkTopic topic = new AiFreeTalkTopic(session.getTopicId(), session.getTitle(), null);
    return new DecisionReservation(
        learningSessionId,
        history.getId(),
        session.getId(),
        submittedMessageId,
        decision,
        learningSession.getTargetLocale().name(),
        learningSession.getBaseLocale().name(),
        partner,
        topic,
        historyMessages(
            sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
                history.getId())));
  }

  /** 종료 확인을 취소한 AI 턴을 저장한다. */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeContinue(
      DecisionReservation reservation, AiFreeTalkTurnResult result) {
    ManagedRecords records =
        managedRecords(
            reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
    records.userMessage().recordInnerThought(result.innerThought(), result.innerThoughtType());
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
        aiMessage);
  }

  /** 종료 확정의 AI 마무리 메시지를 저장하고 세션을 완료한다. */
  @Transactional
  public FreeTalkMessageSubmitResponse finalizeEnd(
      DecisionReservation reservation, AiFreeTalkClosingResult result) {
    ManagedRecords records =
        managedRecords(
            reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
    records.userMessage().recordInnerThought(result.innerThought(), result.innerThoughtType());
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
        aiMessage);
  }

  /** 종료 확인의 AI 호출 실패 뒤 처리 표시만 되돌린다. */
  @Transactional
  public void compensateDecision(DecisionReservation reservation) {
    freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(reservation.learningSessionId())
        .ifPresent(FreeTalkSession::clearProcessing);
  }

  private ManagedRecords managedRecords(Reservation reservation) {
    return managedRecords(
        reservation.learningSessionId(), reservation.historyId(), reservation.userMessageId());
  }

  private ManagedRecords managedRecords(
      long learningSessionId, long historyId, long userMessageId) {
    LearningSession learningSession = requireOwnedSessionWithoutUser(learningSessionId);
    FreeTalkSession session = requireFreeTalkForUpdate(learningSessionId);
    SessionHistory history = sessionHistoryRepository.findById(historyId).orElseThrow();
    SessionHistoryMessage userMessage =
        sessionHistoryMessageRepository.findById(userMessageId).orElseThrow();
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
    return learningSessionRepository.findById(learningSessionId).orElseThrow();
  }

  private FreeTalkSession requireFreeTalkForUpdate(long learningSessionId) {
    return freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
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
      SessionHistoryMessage aiMessage) {
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
            session.getExpressionGenerationStatus()));
  }

  /** AI 호출 전에 저장한 사용자 발화와 대화 문맥이다. */
  public record Reservation(
      long learningSessionId,
      long freeTalkSessionId,
      long historyId,
      long userMessageId,
      String clientMessageId,
      long utteranceDurationMs,
      boolean timeLimitReached,
      boolean firstUserTurn,
      String targetLocale,
      String baseLocale,
      FreeTalkPartner partner,
      AiFreeTalkTopic topic,
      List<AiConversationHistoryMessage> history) {}

  /** 종료 확인 전에 저장한 사용자 메시지와 대화 문맥이다. */
  public record DecisionReservation(
      long learningSessionId,
      long historyId,
      long freeTalkSessionId,
      long userMessageId,
      FreeTalkExitDecision decision,
      String targetLocale,
      String baseLocale,
      FreeTalkPartner partner,
      AiFreeTalkTopic topic,
      List<AiConversationHistoryMessage> history) {}

  private record ManagedRecords(
      long learningSessionId,
      LearningSession learningSession,
      FreeTalkSession freeTalkSession,
      SessionHistory history,
      SessionHistoryMessage userMessage) {}
}
