// 세션 히스토리 Repository를 소유하고 조회와 저장 경계를 제공한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 세션 히스토리 Repository를 소유하고 조회와 저장 경계를 제공한다. */
@Service
@RequiredArgsConstructor
public class SessionHistoryService {

  private final SessionHistoryRepository sessionHistoryRepository;

  /** 학습 세션에 연결된 히스토리를 조회한다. */
  public Optional<SessionHistory> findByLearningSessionId(long learningSessionId) {
    return sessionHistoryRepository.findByLearningSessionId(learningSessionId);
  }

  /** 학습 세션에 연결된 히스토리를 반드시 조회한다. */
  public SessionHistory requireByLearningSessionId(long learningSessionId) {
    return findByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /** 세션 히스토리를 ID로 반드시 조회한다. */
  public SessionHistory require(long sessionHistoryId) {
    return sessionHistoryRepository
        .findById(sessionHistoryId)
        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  /** 세션 히스토리를 저장한다. */
  public SessionHistory save(SessionHistory sessionHistory) {
    return sessionHistoryRepository.save(sessionHistory);
  }

  /** 존재하는 세션 히스토리를 ID로 삭제한다. */
  public void deleteIfExists(long sessionHistoryId) {
    sessionHistoryRepository.findById(sessionHistoryId).ifPresent(sessionHistoryRepository::delete);
  }
}
