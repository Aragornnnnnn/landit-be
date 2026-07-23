// 학습 세션의 소유권, 상태, 잠금 조건을 검증하며 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.SessionType;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학습 세션의 소유권, 상태, 잠금 조건을 검증하며 조회한다. */
@RequiredArgsConstructor
@Service
@Slf4j
public class LearningSessionService {

  private final LearningSessionRepository learningSessionRepository;

  /**
   * 소유자와 진행 상태를 검증하면서 상태 변경 대상 세션을 잠금 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 소유한 진행 중 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 이미 종료됐을 때
   */
  public LearningSession findOwnedInProgressForUpdate(long userId, long sessionId) {
    LearningSession learningSession =
        learningSessionRepository
            .findByIdAndUserProfileIdForUpdate(sessionId, userId)
            .orElseThrow(
                () ->
                    learningSessionRepository.existsById(sessionId)
                        ? new SessionException(SessionErrorCode.FORBIDDEN)
                        : new SessionException(SessionErrorCode.SESSION_NOT_FOUND));
    if (!learningSession.isInProgress()) {
      throw new SessionException(SessionErrorCode.SESSION_ALREADY_COMPLETED);
    }
    return learningSession;
  }

  /**
   * 세션 상태와 무관하게 소유권만 검증해 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 소유한 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없을 때
   */
  public LearningSession findOwned(long userId, long sessionId) {
    return learningSessionRepository
        .findByIdAndUserProfileId(sessionId, userId)
        .orElseThrow(
            () ->
                learningSessionRepository.existsById(sessionId)
                    ? new SessionException(SessionErrorCode.FORBIDDEN)
                    : new SessionException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  /**
   * 소유한 완료 시나리오 세션을 조회하고 최종 피드백 생성 조건을 검증한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 완료된 시나리오 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 완료되지 않았을 때
   */
  public LearningSession findOwnedCompleted(long userId, long sessionId) {
    LearningSession learningSession =
        learningSessionRepository
            .findByIdAndUserProfileId(sessionId, userId)
            .orElseThrow(
                () ->
                    learningSessionRepository.existsById(sessionId)
                        ? new SessionException(SessionErrorCode.FORBIDDEN)
                        : new SessionException(SessionErrorCode.SESSION_NOT_FOUND));
    validateCompletedScenarioSession(learningSession);
    return learningSession;
  }

  /**
   * 최종 피드백 저장 중 세션 결과 확정을 직렬화하며 완료 세션을 잠금 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 잠금 조회한 완료 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 완료되지 않았을 때
   */
  public LearningSession findOwnedCompletedForUpdate(long userId, long sessionId) {
    LearningSession learningSession =
        learningSessionRepository
            .findByIdAndUserProfileIdForUpdate(sessionId, userId)
            .orElseThrow(
                () ->
                    learningSessionRepository.existsById(sessionId)
                        ? new SessionException(SessionErrorCode.FORBIDDEN)
                        : new SessionException(SessionErrorCode.SESSION_NOT_FOUND));
    validateCompletedScenarioSession(learningSession);
    return learningSession;
  }

  /** 최종 피드백을 생성할 수 있는 완료 시나리오 세션인지 검증한다. */
  private void validateCompletedScenarioSession(LearningSession learningSession) {
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED) {
      throw new SessionException(SessionErrorCode.SESSION_NOT_COMPLETED);
    }
    if (learningSession.getSessionType() != SessionType.SCENARIO
        || learningSession.getEndedAt() == null) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 새 학습 세션을 저장한다.
   *
   * @param learningSession 저장할 학습 세션
   * @return 저장된 학습 세션
   */
  public LearningSession save(LearningSession learningSession) {
    return learningSessionRepository.save(learningSession);
  }

  /**
   * 진행 중인 세션을 사용자 중도 종료 상태로 전환한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 종료할 학습 세션 ID
   * @throws SessionException 세션이 없거나 접근할 수 없거나 이미 종료됐을 때
   */
  @Transactional
  public void endSession(long userId, long sessionId) {
    LearningSession learningSession = findOwnedInProgressForUpdate(userId, sessionId);
    learningSession.interruptByUser(LocalDateTime.now());
    log.info("learning session ended by user: userId={}, sessionId={}", userId, sessionId);
  }
}
