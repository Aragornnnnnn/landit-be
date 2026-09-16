// 시나리오 세션과 시작·메시지 컨텍스트 조회 Repository를 소유한다.

package com.landit.landitbe.feature.session.scenario.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.exception.ContentErrorCode;
import com.landit.landitbe.feature.content.scenario.dto.ScenarioStartContext;
import com.landit.landitbe.feature.content.scenario.service.ScenarioContentService;
import com.landit.landitbe.feature.session.scenario.domain.ScenarioSession;
import com.landit.landitbe.feature.session.scenario.repository.ScenarioSessionMessageQueryRepository;
import com.landit.landitbe.feature.session.scenario.repository.ScenarioSessionRepository;
import com.landit.landitbe.feature.session.scenario.repository.projection.ScenarioSessionMessageContextProjection;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 시나리오 세션과 시작·메시지 컨텍스트 조회 Repository를 소유한다. */
@Service
@RequiredArgsConstructor
public class ScenarioSessionService {

  private final ScenarioSessionRepository scenarioSessionRepository;
  private final ScenarioContentService scenarioContentService;
  private final ScenarioSessionMessageQueryRepository messageQueryRepository;

  /**
   * 세션이 사용자가 특정 시각 이후 시작한 해당 시나리오 세션 가운데 처음 완료한 세션인지 확인한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param since 이 시각 이상에 시작한 세션만 센다
   * @param sessionId 확인할 학습 세션 ID
   * @return 처음 완료한 세션과 ID가 같으면 true
   */
  public boolean isFirstCompletedSince(
      long userId, long scenarioId, LocalDateTime since, long sessionId) {
    return scenarioSessionRepository
        .findFirstCompletedSessionIdSince(userId, scenarioId, since)
        .map(firstId -> firstId == sessionId)
        .orElse(false);
  }

  /**
   * 사용자와 시나리오에 맞는 세션 시작 Projection을 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param questionLevelGroup 질문 레벨 그룹
   * @return 세션 시작 Projection
   * @throws ApiException 시나리오 시작 정보를 찾을 수 없을 때
   */
  public ScenarioStartContext requireStartProjection(
      long userId, long scenarioId, ContentLearningLevel questionLevelGroup) {
    return scenarioContentService
        .findStartContext(userId, scenarioId, questionLevelGroup)
        .orElseThrow(() -> new ApiException(ContentErrorCode.SCENARIO_NOT_FOUND));
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
