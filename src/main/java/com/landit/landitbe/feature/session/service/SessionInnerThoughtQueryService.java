// 사용자 메시지 속마음 상태를 조회하고 만료를 처리한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.dto.SessionInnerThoughtResponse;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 메시지 속마음 상태를 조회하고 만료를 처리한다. */
@RequiredArgsConstructor
@Service
public class SessionInnerThoughtQueryService {

  private static final long PREPARING_TIMEOUT_SECONDS = 90;

  private final LearningSessionService learningSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final SessionMessageService sessionMessageService;

  /** 소유한 사용자 메시지의 속마음 처리 상태와 완료 결과를 반환한다. */
  @Transactional
  public SessionInnerThoughtResponse get(long userId, long sessionId, long messageId) {
    learningSessionService.findOwned(userId, sessionId);
    SessionHistory sessionHistory = sessionHistoryService.requireByLearningSessionId(sessionId);
    SessionHistoryMessage message = findUserMessage(sessionHistory.getId(), messageId);
    if (isStalePreparing(message)) {
      int updated = sessionMessageService.failInnerThought(messageId);
      if (updated == 1) {
        return SessionInnerThoughtResponse.failed();
      }
      message = findUserMessage(sessionHistory.getId(), messageId);
    }
    return SessionInnerThoughtResponse.from(message);
  }

  private SessionHistoryMessage findUserMessage(long sessionHistoryId, long messageId) {
    SessionHistoryMessage message =
        sessionMessageService.requireInHistory(messageId, sessionHistoryId);
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
}
