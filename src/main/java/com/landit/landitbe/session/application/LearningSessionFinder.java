// 메시지 제출 흐름에서 학습 세션 조회와 잠금 검증을 담당한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.domain.LearningSession;
import com.landit.landitbe.session.infrastructure.LearningSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class LearningSessionFinder {

    private final LearningSessionRepository learningSessionRepository;

    /**
     * 소유자와 진행 상태를 검증하면서 메시지 제출 대상 세션을 잠금 조회한다.
     */
    LearningSession findOwnedInProgressForUpdate(long userId, long sessionId) {
        LearningSession learningSession = learningSessionRepository
                .findByIdAndUserProfileIdForUpdate(sessionId, userId)
                .orElseThrow(() -> learningSessionRepository.existsById(sessionId)
                        ? new ApiException(ErrorCode.FORBIDDEN)
                        : new ApiException(ErrorCode.SESSION_NOT_FOUND));
        if (!learningSession.isInProgress()) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
        return learningSession;
    }
}
