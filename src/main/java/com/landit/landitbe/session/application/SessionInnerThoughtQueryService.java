// 사용자 메시지 속마음 상태를 조회하고 만료를 처리한다.

package com.landit.landitbe.session.application;

import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.api.dto.SessionInnerThoughtResponse;
import com.landit.landitbe.session.domain.ProcessingStatus;
import com.landit.landitbe.session.domain.SessionHistory;
import com.landit.landitbe.session.domain.SessionHistoryMessage;
import com.landit.landitbe.session.infrastructure.SessionHistoryMessageRepository;
import com.landit.landitbe.session.infrastructure.SessionHistoryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 메시지 속마음 상태를 조회하고 만료를 처리한다. */
@RequiredArgsConstructor
@Service
public class SessionInnerThoughtQueryService {

  private static final long PREPARING_TIMEOUT_SECONDS = 90;

  private final LearningSessionFinder learningSessionFinder;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** 소유한 사용자 메시지의 속마음 처리 상태와 완료 결과를 반환한다. */
  @Transactional
  public SessionInnerThoughtResponse get(long userId, long sessionId, long messageId) {
    learningSessionFinder.findOwned(userId, sessionId);
    SessionHistory sessionHistory =
        sessionHistoryRepository
            .findByLearningSessionId(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    SessionHistoryMessage message = findUserMessage(sessionHistory.getId(), messageId);
    if (isStalePreparing(message)) {
      int updated =
          sessionHistoryMessageRepository.markInnerThoughtFailedIfPreparing(
              messageId, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
      if (updated == 1) {
        return new SessionInnerThoughtResponse(ProcessingStatus.FAILED.name(), null, null);
      }
      message = findUserMessage(sessionHistory.getId(), messageId);
    }
    return toResponse(message);
  }

  private SessionHistoryMessage findUserMessage(long sessionHistoryId, long messageId) {
    SessionHistoryMessage message =
        sessionHistoryMessageRepository
            .findByIdAndSessionHistoryId(messageId, sessionHistoryId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    if (message.getRole() != ConversationSpeaker.USER) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return message;
  }

  private boolean isStalePreparing(SessionHistoryMessage message) {
    return message.getInnerThoughtProcessingStatus() == ProcessingStatus.PREPARING
        && !message
            .getCreatedAt()
            .plusSeconds(PREPARING_TIMEOUT_SECONDS)
            .isAfter(LocalDateTime.now());
  }

  private SessionInnerThoughtResponse toResponse(SessionHistoryMessage message) {
    ProcessingStatus processingStatus = message.getInnerThoughtProcessingStatus();
    if (processingStatus != ProcessingStatus.COMPLETED) {
      return new SessionInnerThoughtResponse(processingStatus.name(), null, null);
    }
    return new SessionInnerThoughtResponse(
        processingStatus.name(), message.getInnerThought(), message.getInnerThoughtType().name());
  }
}
