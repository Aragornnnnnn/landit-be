// 세션 히스토리 Repository를 소유하고 조회와 저장 경계를 제공한다.

package com.landit.landitbe.feature.learning.conversation.history.service;

import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.history.domain.SessionHistory;
import com.landit.landitbe.feature.learning.conversation.history.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 세션 히스토리 Repository를 소유하고 조회와 저장 경계를 제공한다. */
@Service
@RequiredArgsConstructor
public class SessionHistoryService {

  private final SessionHistoryRepository sessionHistoryRepository;

  /**
   * 학습 세션에 연결된 히스토리를 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 세션 히스토리
   */
  public Optional<SessionHistorySnapshot> findByLearningSessionId(long learningSessionId) {
    return sessionHistoryRepository
        .findByLearningSessionId(learningSessionId)
        .map(SessionHistorySnapshot::from);
  }

  /**
   * 학습 세션에 연결된 히스토리를 반드시 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 세션 히스토리
   * @throws ApiException 히스토리가 없을 때
   */
  public SessionHistorySnapshot requireByLearningSessionId(long learningSessionId) {
    return findByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /**
   * 세션 히스토리를 ID로 반드시 조회한다.
   *
   * @param sessionHistoryId 세션 히스토리 ID
   * @return 세션 히스토리
   * @throws ApiException 히스토리가 없을 때
   */
  public SessionHistorySnapshot require(long sessionHistoryId) {
    return sessionHistoryRepository
        .findById(sessionHistoryId)
        .map(SessionHistorySnapshot::from)
        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  /**
   * 존재하는 세션 히스토리를 ID로 삭제한다.
   *
   * @param sessionHistoryId 삭제할 세션 히스토리 ID
   */
  public void deleteIfExists(long sessionHistoryId) {
    sessionHistoryRepository.findById(sessionHistoryId).ifPresent(sessionHistoryRepository::delete);
  }

  /**
   * 학습 시작에 연결된 대화 기록을 저장한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @param userProfileId 사용자 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param startedAt 시작 시각
   * @return 저장한 대화 기록
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistorySnapshot startScenario(
      Long learningSessionId,
      Long userProfileId,
      Locale targetLocale,
      Locale baseLocale,
      LocalDateTime startedAt) {
    return SessionHistorySnapshot.from(
        sessionHistoryRepository.save(
            SessionHistory.startedScenario(
                learningSessionId, userProfileId, targetLocale, baseLocale, startedAt)));
  }

  /**
   * 학습 시작에 연결된 대화 기록을 저장한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @param userProfileId 사용자 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param startedAt 시작 시각
   * @return 저장한 대화 기록
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistorySnapshot startFreeTalk(
      Long learningSessionId,
      Long userProfileId,
      Locale targetLocale,
      Locale baseLocale,
      LocalDateTime startedAt) {
    return SessionHistorySnapshot.from(
        sessionHistoryRepository.save(
            SessionHistory.startedFreeTalk(
                learningSessionId, userProfileId, targetLocale, baseLocale, startedAt)));
  }

  /**
   * 대화 기록의 종료 시각과 발화 수를 확정한다.
   *
   * @param historyId 대화 기록 ID
   * @param endedAt 종료 시각
   * @param userMessageCount 사용자 발화 수
   * @return 완료한 대화 기록
   * @throws ApiException 기록이 없을 때
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistorySnapshot complete(
      long historyId, LocalDateTime endedAt, int userMessageCount) {
    SessionHistory history =
        sessionHistoryRepository
            .findById(historyId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    history.complete(endedAt, userMessageCount);
    return SessionHistorySnapshot.from(history);
  }

  /**
   * 대화 기록을 ID로 조회한다.
   *
   * @param historyId 대화 기록 ID
   * @return 존재하는 대화 기록
   */
  public Optional<SessionHistorySnapshot> findHistory(long historyId) {
    return sessionHistoryRepository.findById(historyId).map(SessionHistorySnapshot::from);
  }

  /**
   * 시작 실패로 생성한 대화 기록을 제거하고 DB에 반영한다.
   *
   * @param historyId 삭제할 기록 ID
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void deleteStart(long historyId) {
    deleteIfExists(historyId);
    sessionHistoryRepository.flush();
  }
}
