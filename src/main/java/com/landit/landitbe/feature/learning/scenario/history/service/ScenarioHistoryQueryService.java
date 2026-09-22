// 완료한 시나리오 회차를 로그인 사용자 기준으로 조회한다.

package com.landit.landitbe.feature.learning.scenario.history.service;

import com.landit.landitbe.feature.learning.scenario.history.dto.ScenarioHistoryResponse;
import com.landit.landitbe.feature.learning.scenario.history.repository.ScenarioHistoryQueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 완료 회차를 최신순으로 조회한다. */
@Service
@RequiredArgsConstructor
public class ScenarioHistoryQueryService {
  private final ScenarioHistoryQueryRepository histories;

  /**
   * 로그인 사용자가 완료한 시나리오의 모든 회차를 조회한다.
   *
   * @param userId 로그인 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 완료 시각 내림차순의 회차 목록. 기록이 없으면 빈 목록
   */
  @Transactional(readOnly = true)
  public ScenarioHistoryResponse findHistory(long userId, long scenarioId) {
    return new ScenarioHistoryResponse(
        scenarioId,
        histories.findCompleted(userId, scenarioId).stream()
            .map(
                session ->
                    new ScenarioHistoryResponse.Session(
                        session.sessionId(),
                        session.startedAt(),
                        session.endedAt(),
                        List.of(),
                        null))
            .toList());
  }
}
