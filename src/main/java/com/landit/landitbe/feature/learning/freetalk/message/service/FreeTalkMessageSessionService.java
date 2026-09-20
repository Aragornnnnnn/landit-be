// 프리톡 메시지 처리에서 세션 소유권과 잠금 및 처리 만료를 관리한다.

package com.landit.landitbe.feature.learning.freetalk.message.service;

import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 프리톡 메시지 처리에서 세션 소유권과 잠금 및 처리 만료를 관리한다. */
@Service
@RequiredArgsConstructor
class FreeTalkMessageSessionService {

  private static final long PROCESSING_TIMEOUT_SECONDS = 90;
  private final LearningSessionService learningSessionService;
  private final FreeTalkSessionRepository freeTalkSessionRepository;

  LearningSessionSnapshot requireOwnedSession(long userId, long learningSessionId) {
    LearningSessionSnapshot session =
        learningSessionService
            .findSession(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    if (!Long.valueOf(userId).equals(session.getUserProfileId())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    return learningSessionService
        .lockOwnedSnapshot(learningSessionId, userId)
        .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  FreeTalkSession requireFreeTalkForUpdate(long learningSessionId) {
    return freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(learningSessionId)
        .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  void clearExpiredProcessing(FreeTalkSession session) {
    session.clearProcessingIfExpired(LocalDateTime.now().minusSeconds(PROCESSING_TIMEOUT_SECONDS));
  }
}
