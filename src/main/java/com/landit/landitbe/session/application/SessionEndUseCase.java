// 학습 세션 중도 종료 유스케이스를 처리한다.

package com.landit.landitbe.session.application;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.domain.LearningSession;
import com.landit.landitbe.session.infrastructure.LearningSessionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학습 세션 중도 종료 유스케이스를 처리한다. */
@RequiredArgsConstructor
@Service
public class SessionEndUseCase {

  private final LearningSessionRepository learningSessionRepository;

  /** 진행 중인 세션을 사용자 중도 종료 상태로 전환한다. */
  @Transactional
  public void endSession(long userId, long sessionId) {
    // 세션은 존재하지만 소유자가 다르면 403,
    // 세션 자체가 없으면 404로 구분한다.
    LearningSession learningSession =
        learningSessionRepository
            .findByIdAndUserProfileId(sessionId, userId)
            .orElseThrow(
                () ->
                    learningSessionRepository.existsById(sessionId)
                        ? new ApiException(ErrorCode.FORBIDDEN)
                        : new ApiException(ErrorCode.SESSION_NOT_FOUND));
    // 완료, 중단 등 이미 진행 중이 아닌 세션은 다시 종료하지 않는다.
    if (!learningSession.isInProgress()) {
      throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
    }
    learningSession.interruptByUser(LocalDateTime.now());
  }
}
