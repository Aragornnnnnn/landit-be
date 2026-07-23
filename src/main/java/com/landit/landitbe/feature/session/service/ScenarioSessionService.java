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

  /**
   * 사용자와 시나리오에 맞는 세션 시작 Projection을 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 세션 시작 Projection
   * @throws ApiException 시나리오 시작 정보를 찾을 수 없을 때
   */
  public ScenarioSessionStartProjection requireStartProjection(long userId, long scenarioId) {
    return startQueryRepository
        .findStartRow(userId, scenarioId)
        .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));
  }

  /**
   * 직전 시나리오 잠금 상태를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시작할 시나리오 ID
   * @return 직전 시나리오 잠금 상태
   */
  public Optional<ScenarioSessionLockProjection> findPreviousScenarioLock(
      long userId, long scenarioId) {
    return startQueryRepository.findPreviousScenarioLockRow(userId, scenarioId);
  }

  /**
   * 시나리오 세션을 저장한다.
   *
   * @param scenarioSession 저장할 시나리오 세션
   * @return 저장된 시나리오 세션
   */
  public ScenarioSession save(ScenarioSession scenarioSession) {
    return scenarioSessionRepository.save(scenarioSession);
  }

  /**
   * 학습 세션에 연결된 시나리오 세션을 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 연결된 시나리오 세션
   * @throws ApiException 시나리오 세션 연결 정보가 없을 때
   */
  public ScenarioSession requireByLearningSessionId(long learningSessionId) {
    return scenarioSessionRepository
        .findByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  /**
   * AI 요청에 필요한 시나리오 메시지 컨텍스트를 조회한다.
   *
   * @param learningSessionId 학습 세션 ID
   * @return 시나리오 메시지 컨텍스트
   * @throws ApiException 메시지 생성에 필요한 시나리오 컨텍스트가 없을 때
   */
  public ScenarioSessionMessageContextProjection requireMessageContext(long learningSessionId) {
    return messageQueryRepository
        .findContextByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
