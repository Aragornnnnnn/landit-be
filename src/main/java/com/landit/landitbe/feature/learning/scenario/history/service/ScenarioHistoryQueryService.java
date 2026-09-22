// 시나리오 완료 회차에 저장 당시의 대화와 속마음을 연결한다.

package com.landit.landitbe.feature.learning.scenario.history.service;

import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.scenario.history.dto.ScenarioHistoryResponse;
import com.landit.landitbe.feature.learning.scenario.history.repository.ScenarioHistoryQueryRepository;
import com.landit.landitbe.feature.learning.scenario.history.repository.projection.ScenarioHistoryProjection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 완료 회차와 저장된 대화를 조회한다. */
@Service
@RequiredArgsConstructor
public class ScenarioHistoryQueryService {
  private final ScenarioHistoryQueryRepository histories;
  private final ConversationMessageService messages;

  /**
   * 로그인 사용자가 완료한 시나리오의 모든 회차와 대화를 조회한다.
   *
   * @param userId 로그인 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 완료 시각 내림차순의 회차와 대화 목록
   */
  @Transactional(readOnly = true)
  public ScenarioHistoryResponse findHistory(long userId, long scenarioId) {
    List<ScenarioHistoryProjection> sessions = histories.findCompleted(userId, scenarioId);
    if (sessions.isEmpty()) {
      return new ScenarioHistoryResponse(scenarioId, List.of());
    }
    List<Long> historyIds = sessions.stream().map(ScenarioHistoryProjection::historyId).toList();
    var messagesByHistory =
        messages.findAllByHistoryIds(historyIds).stream()
            .collect(Collectors.groupingBy(SessionHistoryMessageSnapshot::sessionHistoryId));
    return new ScenarioHistoryResponse(
        scenarioId,
        sessions.stream()
            .map(
                session ->
                    new ScenarioHistoryResponse.Session(
                        session.sessionId(),
                        session.startedAt(),
                        session.endedAt(),
                        messagesByHistory.getOrDefault(session.historyId(), List.of()).stream()
                            .map(ScenarioHistoryResponse.Message::from)
                            .toList(),
                        null))
            .toList());
  }
}
