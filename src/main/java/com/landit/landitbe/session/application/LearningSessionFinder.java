// 학습 세션의 소유권, 상태, 잠금 조건을 검증하며 조회한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.domain.LearningSession;
import com.landit.landitbe.session.domain.LearningSessionStatus;
import com.landit.landitbe.session.domain.SessionType;
import com.landit.landitbe.session.infrastructure.LearningSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class LearningSessionFinder {

    private final LearningSessionRepository learningSessionRepository;

    /** 소유자와 진행 상태를 검증하면서 메시지 제출 대상 세션을 잠금 조회한다. */
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

    /** 소유한 완료 시나리오 세션을 조회하고 최종 피드백 생성 조건을 검증한다. */
    LearningSession findOwnedCompleted(long userId, long sessionId) {
        LearningSession learningSession = learningSessionRepository
                .findByIdAndUserProfileId(sessionId, userId)
                .orElseThrow(() -> learningSessionRepository.existsById(sessionId)
                        ? new ApiException(ErrorCode.FORBIDDEN)
                        : new ApiException(ErrorCode.SESSION_NOT_FOUND));
        validateCompletedScenarioSession(learningSession);
        return learningSession;
    }

    /** 최종 피드백 저장 중 세션 결과 확정을 직렬화하며 완료 세션을 잠금 조회한다. */
    LearningSession findOwnedCompletedForUpdate(long userId, long sessionId) {
        LearningSession learningSession = learningSessionRepository
                .findByIdAndUserProfileIdForUpdate(sessionId, userId)
                .orElseThrow(() -> learningSessionRepository.existsById(sessionId)
                        ? new ApiException(ErrorCode.FORBIDDEN)
                        : new ApiException(ErrorCode.SESSION_NOT_FOUND));
        validateCompletedScenarioSession(learningSession);
        return learningSession;
    }

    /** 최종 피드백을 생성할 수 있는 완료 시나리오 세션인지 검증한다. */
    private void validateCompletedScenarioSession(LearningSession learningSession) {
        if (learningSession.getStatus() != LearningSessionStatus.COMPLETED) {
            throw new ApiException(ErrorCode.SESSION_NOT_COMPLETED);
        }
        if (learningSession.getSessionType() != SessionType.SCENARIO
                || learningSession.getEndedAt() == null) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
