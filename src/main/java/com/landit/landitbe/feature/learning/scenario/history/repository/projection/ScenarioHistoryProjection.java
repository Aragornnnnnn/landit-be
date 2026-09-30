// 완료한 시나리오 회차의 저장 당시 조회 정보를 전달한다.

package com.landit.landitbe.feature.learning.scenario.history.repository.projection;

import java.time.LocalDateTime;

/**
 * 완료한 시나리오 회차의 조회 값이다.
 *
 * @param sessionId 학습 세션 ID
 * @param historyId 대화 이력 ID
 * @param startedAt 시작 시각
 * @param endedAt 완료 시각
 * @param userOpeningInstruction 사용자 선톡 시작 안내 스냅샷
 */
public record ScenarioHistoryProjection(
    Long sessionId,
    Long historyId,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    String userOpeningInstruction) {}
