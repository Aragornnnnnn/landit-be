// 시나리오 세션과 시작·메시지 컨텍스트 조회 Repository를 소유한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.ScenarioSession;
import com.landit.landitbe.feature.session.repository.ScenarioSessionMessageQueryRepository;
import com.landit.landitbe.feature.session.repository.ScenarioSessionRepository;
import com.landit.landitbe.feature.session.repository.ScenarioSessionStartQueryRepository;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionLockProjection;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.feature.session.repository.projection.ScenarioSessionStartProjection;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 시나리오 세션과 시작·메시지 컨텍스트 조회 Repository를 소유한다. */
@Service
@RequiredArgsConstructor
public class ScenarioSessionService {

  private final ScenarioSessionRepository scenarioSessionRepository;
  private final ScenarioSessionStartQueryRepository startQueryRepository;
  private final ScenarioSessionMessageQueryRepository messageQueryRepository;

  /** 사용자와 시나리오에 맞는 세션 시작 Projection을 조회한다. */
  public ScenarioSessionStartProjection requireStartProjection(long userId, long scenarioId) {
    return startQueryRepository
        .findStartRow(userId, scenarioId)
        .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));
  }

  /** 직전 시나리오 잠금 상태를 조회한다. */
  public Optional<ScenarioSessionLockProjection> findPreviousScenarioLock(
      long userId, long scenarioId) {
    return startQueryRepository.findPreviousScenarioLockRow(userId, scenarioId);
  }

  /** 시나리오 세션을 저장한다. */
  public ScenarioSession save(ScenarioSession scenarioSession) {
    return scenarioSessionRepository.save(scenarioSession);
  }

  /** 학습 세션에 연결된 시나리오 세션을 조회한다. */
  public ScenarioSession requireByLearningSessionId(long learningSessionId) {
    return scenarioSessionRepository
        .findByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  /** AI 요청에 필요한 시나리오 메시지 컨텍스트를 조회한다. */
  public ScenarioSessionMessageContextProjection requireMessageContext(long learningSessionId) {
    return messageQueryRepository
        .findContextByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }
}
