// 프리톡의 완료된 메시지와 종료 결정을 재요청할 때 저장된 결과를 복원한다.

package com.landit.landitbe.feature.session.freetalk.message.service;

import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.freetalk.message.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.freetalk.message.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.session.history.domain.SessionHistory;
import com.landit.landitbe.feature.session.history.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.history.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.history.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡의 완료된 메시지와 종료 결정을 재요청할 때 저장된 결과를 복원한다. */
@Service
@RequiredArgsConstructor
public class FreeTalkMessageReplayService {

  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final FreeTalkMessageSessionService sessionService;
  private final FreeTalkMessageResponseService responseService;

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
    sessionService.requireOwnedSession(userId, learningSessionId);
    FreeTalkSession session = sessionService.requireFreeTalkForUpdate(learningSessionId);
    sessionService.clearExpiredProcessing(session);
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
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
      return responseService.buildReplayResponse(
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
    return responseService.buildReplayResponse(
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
    sessionService.requireOwnedSession(userId, learningSessionId);
    FreeTalkSession session = sessionService.requireFreeTalkForUpdate(learningSessionId);
    sessionService.clearExpiredProcessing(session);
    if (session.getProcessingClientMessageId() != null) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
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
    return responseService.buildReplayResponse(
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

  // 저장된 사용자 메시지 바로 다음의 AI 메시지를 조회한다.
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

  // 저장된 턴 상태를 멱등 응답에 사용할 대화 상태로 변환한다.
  private FreeTalkConversationStatus replayedConversationStatus(
      FreeTalkTurnStatus storedTurnStatus) {
    return storedTurnStatus == FreeTalkTurnStatus.COMPLETED
        ? FreeTalkConversationStatus.COMPLETED
        : FreeTalkConversationStatus.IN_PROGRESS;
  }

  private int indexOfMessage(List<SessionHistoryMessage> messages, long messageId) {
    for (int index = 0; index < messages.size(); index++) {
      if (Long.valueOf(messageId).equals(messages.get(index).getId())) {
        return index;
      }
    }
    throw new ApiException(SessionErrorCode.SESSION_NOT_FOUND);
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
}
