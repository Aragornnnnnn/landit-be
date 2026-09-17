// 학습 세션의 소유권, 상태, 잠금 조건을 검증하며 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.CompletionReason;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.SessionType;
import com.landit.landitbe.feature.session.dto.LearningSessionAccess;
import com.landit.landitbe.feature.session.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
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
   * 여러 세션을 한 번의 조회로 읽는다.
   *
   * @param sessionIds 조회할 세션 ID 목록
   * @return 존재하는 세션의 조회 상태
   */
  public java.util.List<LearningSessionSnapshot> findSessions(java.util.List<Long> sessionIds) {
    return learningSessionRepository.findAllById(sessionIds).stream()
        .map(LearningSessionSnapshot::from)
        .toList();
  }

  /**
   * 소유자와 진행 상태를 검증하면서 상태 변경 대상 세션을 잠금 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 소유한 진행 중 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 이미 종료됐을 때
   */
  private LearningSession loadOwnedInProgressForUpdate(long userId, long sessionId) {
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
  private LearningSession loadOwned(long userId, long sessionId) {
    return learningSessionRepository
        .findByIdAndUserProfileId(sessionId, userId)
        .orElseThrow(
            () ->
                learningSessionRepository.existsById(sessionId)
                    ? new SessionException(SessionErrorCode.FORBIDDEN)
                    : new SessionException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  /**
   * 상태와 무관하게 소유 세션을 잠가 재개와 중복 요청을 직렬화한다.
   *
   * @param userId 소유자 ID
   * @param sessionId 세션 ID
   * @return 잠근 소유 세션
   * @throws SessionException 소유 세션이 없을 때
   */
  private LearningSession loadOwnedForUpdate(long userId, long sessionId) {
    return learningSessionRepository
        .findByIdAndUserProfileIdForUpdate(sessionId, userId)
        .orElseThrow(
            () ->
                learningSessionRepository.existsById(sessionId)
                    ? new SessionException(SessionErrorCode.FORBIDDEN)
                    : new SessionException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  /** 존재하는 소유 세션만 반환해 필터가 다른 계정의 대상에 접근하지 않게 한다. */
  public java.util.Optional<LearningSessionAccess> findOwnedIfPresent(long userId, long sessionId) {
    return learningSessionRepository
        .findByIdAndUserProfileId(sessionId, userId)
        .map(
            session ->
                new LearningSessionAccess(
                    session.getSessionType(), session.getStatus(), session.getStartedAt()));
  }

  /**
   * 소유한 완료 시나리오 세션을 조회하고 최종 피드백 생성 조건을 검증한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 완료된 시나리오 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 완료되지 않았을 때
   */
  private LearningSession loadOwnedCompleted(long userId, long sessionId) {
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
  private LearningSession loadOwnedCompletedForUpdate(long userId, long sessionId) {
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

  /**
   * 평가 대상 세션이 사용자의 최신 완료 시나리오인지 확인한다.
   *
   * @param session 평가할 소유 세션
   * @return 최신 완료 시나리오와 ID가 같으면 true
   */
  public boolean isLatestCompletedScenario(LearningSessionSnapshot session) {
    return learningSessionRepository
        .findTopByUserProfileIdAndSessionTypeAndStatusOrderByEndedAtDescIdDesc(
            session.getUserProfileId(), SessionType.SCENARIO, LearningSessionStatus.COMPLETED)
        .map(latest -> latest.getId().equals(session.getId()))
        .orElse(false);
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
  private LearningSession save(LearningSession learningSession) {
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
    LearningSession learningSession = loadOwnedInProgressForUpdate(userId, sessionId);
    learningSession.interruptByUser(LocalDateTime.now());
    log.info("learning session ended by user: userId={}, sessionId={}", userId, sessionId);
  }

  /**
   * 요청자가 소유한 지정 종류 세션의 시작 시각을 제공한다.
   *
   * @param userId 요청 사용자
   * @param sessionId 세션 ID
   * @param kind 세션 종류
   * @return 소유자와 종류가 일치하는 세션 시작 시각
   */
  @Transactional(readOnly = true)
  public Optional<java.time.LocalDateTime> findOwnedStart(
      long userId, long sessionId, String kind) {
    return findOwnedIfPresent(userId, sessionId)
        .filter(session -> session.sessionType().name().equals(kind))
        .map(LearningSessionAccess::startedAt);
  }

  /**
   * 저장 응답의 재전송 대상이 요청자의 시나리오인지 확인한다.
   *
   * @param userId 요청 사용자
   * @param sessionId 세션 ID
   * @return 소유한 시나리오 세션이면 true
   */
  @Transactional(readOnly = true)
  public boolean ownsScenario(long userId, long sessionId) {
    return findOwnedIfPresent(userId, sessionId)
        .filter(session -> session.sessionType().name().equals("SCENARIO"))
        .isPresent();
  }

  /**
   * 결과 재생성 대상이 요청자의 완료된 프리톡인지 확인한다.
   *
   * @param userId 요청 사용자
   * @param sessionId 세션 ID
   * @return 소유한 완료 프리톡이면 true
   */
  @Transactional(readOnly = true)
  public boolean ownsCompletedFreeTalk(long userId, long sessionId) {
    return findOwnedIfPresent(userId, sessionId)
        .filter(session -> session.sessionType().name().equals("FREE_TALK"))
        .filter(session -> session.status() == LearningSessionStatus.COMPLETED)
        .isPresent();
  }

  /**
   * 소유자와 진행 상태를 검증하면서 상태 변경 대상 세션을 잠금 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 소유한 진행 중 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 이미 종료됐을 때
   */
  public LearningSessionSnapshot findOwnedInProgressForUpdate(long userId, long sessionId) {
    return LearningSessionSnapshot.from(loadOwnedInProgressForUpdate(userId, sessionId));
  }

  /**
   * 세션 상태와 무관하게 소유권만 검증해 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 소유한 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없을 때
   */
  public LearningSessionSnapshot findOwned(long userId, long sessionId) {
    return LearningSessionSnapshot.from(loadOwned(userId, sessionId));
  }

  /**
   * 상태와 무관하게 소유 세션을 잠가 재개와 중복 요청을 직렬화한다.
   *
   * @param userId 소유자 ID
   * @param sessionId 세션 ID
   * @return 잠근 소유 세션
   * @throws SessionException 소유 세션이 없을 때
   */
  public LearningSessionSnapshot findOwnedForUpdate(long userId, long sessionId) {
    return LearningSessionSnapshot.from(loadOwnedForUpdate(userId, sessionId));
  }

  /**
   * 소유한 완료 시나리오 세션을 조회하고 최종 피드백 생성 조건을 검증한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 완료된 시나리오 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 완료되지 않았을 때
   */
  public LearningSessionSnapshot findOwnedCompleted(long userId, long sessionId) {
    return LearningSessionSnapshot.from(loadOwnedCompleted(userId, sessionId));
  }

  /**
   * 최종 피드백 저장 중 세션 결과 확정을 직렬화하며 완료 세션을 잠금 조회한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 학습 세션 ID
   * @return 잠금 조회한 완료 학습 세션
   * @throws SessionException 세션이 없거나 접근할 수 없거나 완료되지 않았을 때
   */
  public LearningSessionSnapshot findOwnedCompletedForUpdate(long userId, long sessionId) {
    return LearningSessionSnapshot.from(loadOwnedCompletedForUpdate(userId, sessionId));
  }

  /**
   * 학습 세션을 시작하고 저장한 상태를 반환한다.
   *
   * @param userProfileId 사용자 ID
   * @param aiTutorId 튜터 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param startedAt 시작 시각
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot startScenario(
      Long userProfileId,
      Long aiTutorId,
      Locale targetLocale,
      Locale baseLocale,
      LocalDateTime startedAt) {
    return LearningSessionSnapshot.from(
        save(
            LearningSession.startScenario(
                userProfileId, aiTutorId, targetLocale, baseLocale, startedAt)));
  }

  /**
   * 학습 세션을 시작하고 저장한 상태를 반환한다.
   *
   * @param userProfileId 사용자 ID
   * @param aiTutorId 튜터 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param startedAt 시작 시각
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot startFreeTalk(
      Long userProfileId,
      Long aiTutorId,
      Locale targetLocale,
      Locale baseLocale,
      LocalDateTime startedAt) {
    return LearningSessionSnapshot.from(
        save(
            LearningSession.startFreeTalk(
                userProfileId, aiTutorId, targetLocale, baseLocale, startedAt)));
  }

  /**
   * 호출자가 잠근 세션 상태를 변경하고 갱신한 값을 반환한다.
   *
   * @param sessionId 이미 검증한 세션 ID
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot resumeInterruptedScenario(long sessionId) {
    LearningSession session = requireEntity(sessionId);
    session.resumeInterruptedScenario();
    return LearningSessionSnapshot.from(session);
  }

  /**
   * 호출자가 잠근 세션 상태를 변경하고 갱신한 값을 반환한다.
   *
   * @param sessionId 이미 검증한 세션 ID
   * @param reason 종료 사유
   * @param endedAt 종료 시각
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot completeBySystem(
      long sessionId, CompletionReason reason, LocalDateTime endedAt) {
    LearningSession session = requireEntity(sessionId);
    session.completeBySystem(reason, endedAt);
    return LearningSessionSnapshot.from(session);
  }

  /**
   * 호출자가 잠근 세션 상태를 변경하고 갱신한 값을 반환한다.
   *
   * @param sessionId 이미 검증한 세션 ID
   * @param endedAt 종료 시각
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot completeFreeTalkByUser(long sessionId, LocalDateTime endedAt) {
    LearningSession session = requireEntity(sessionId);
    session.completeFreeTalkByUser(endedAt);
    return LearningSessionSnapshot.from(session);
  }

  /**
   * 호출자가 잠근 세션 상태를 변경하고 갱신한 값을 반환한다.
   *
   * @param sessionId 이미 검증한 세션 ID
   * @param endedAt 종료 시각
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot completeFreeTalkByTimeLimit(
      long sessionId, LocalDateTime endedAt) {
    LearningSession session = requireEntity(sessionId);
    session.completeFreeTalkByTimeLimit(endedAt);
    return LearningSessionSnapshot.from(session);
  }

  /**
   * 호출자가 잠근 세션 상태를 변경하고 갱신한 값을 반환한다.
   *
   * @param sessionId 이미 검증한 세션 ID
   * @param requestedAt 평가 요청 시각
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot prepareLevelAssessment(long sessionId, LocalDateTime requestedAt) {
    LearningSession session = requireEntity(sessionId);
    session.prepareLevelAssessment(requestedAt);
    return LearningSessionSnapshot.from(session);
  }

  /**
   * 호출자가 잠근 세션 상태를 변경하고 갱신한 값을 반환한다.
   *
   * @param sessionId 이미 검증한 세션 ID
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot completeLevelAssessment(long sessionId) {
    LearningSession session = requireEntity(sessionId);
    session.completeLevelAssessment();
    return LearningSessionSnapshot.from(session);
  }

  /**
   * 호출자가 잠근 세션 상태를 변경하고 갱신한 값을 반환한다.
   *
   * @param sessionId 이미 검증한 세션 ID
   * @return 조회 또는 변경한 세션 상태
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public LearningSessionSnapshot failLevelAssessment(long sessionId) {
    LearningSession session = requireEntity(sessionId);
    session.failLevelAssessment();
    return LearningSessionSnapshot.from(session);
  }

  /**
   * 세션 존재 여부와 상태를 조회한다.
   *
   * @param sessionId 세션 ID
   * @return 조회 또는 변경한 세션 상태
   */
  public Optional<LearningSessionSnapshot> findSession(long sessionId) {
    return learningSessionRepository.findById(sessionId).map(LearningSessionSnapshot::from);
  }

  /**
   * 소유 세션의 상태를 제공한다.
   *
   * @param sessionId 세션 ID
   * @param userId 소유자 ID
   * @return 조회 또는 변경한 세션 상태
   */
  public Optional<LearningSessionSnapshot> findOwnedSnapshot(long sessionId, long userId) {
    return learningSessionRepository
        .findByIdAndUserProfileId(sessionId, userId)
        .map(LearningSessionSnapshot::from);
  }

  /**
   * 소유 세션의 상태를 제공한다.
   *
   * @param sessionId 세션 ID
   * @param userId 소유자 ID
   * @return 조회 또는 변경한 세션 상태
   */
  public Optional<LearningSessionSnapshot> lockOwnedSnapshot(long sessionId, long userId) {
    return learningSessionRepository
        .findByIdAndUserProfileIdForUpdate(sessionId, userId)
        .map(LearningSessionSnapshot::from);
  }

  /**
   * 세션의 존재 여부를 확인한다.
   *
   * @param sessionId 세션 ID
   * @return 조회 또는 변경한 세션 상태
   */
  public boolean exists(long sessionId) {
    return learningSessionRepository.existsById(sessionId);
  }

  /**
   * 실패한 시작 레코드를 삭제하고 DB에 반영한다.
   *
   * @param sessionId 시작 중 실패한 세션 ID
   */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void deleteStart(long sessionId) {
    learningSessionRepository.deleteById(sessionId);
    learningSessionRepository.flush();
  }

  private LearningSession requireEntity(long sessionId) {
    return learningSessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
  }
}
