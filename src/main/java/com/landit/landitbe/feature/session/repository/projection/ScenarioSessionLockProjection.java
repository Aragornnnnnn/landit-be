// 시나리오 순차 잠금 계산에 필요한 최소 조회 결과를 담는다.

package com.landit.landitbe.feature.session.repository.projection;

import com.landit.landitbe.feature.learning.domain.UserScenarioProgressStatus;

/**
 * 시나리오 순차 잠금 계산에 필요한 최소 조회 결과를 담는다.
 *
 * @param scenarioId 시나리오 ID
 * @param progressStatus 학습 진행 상태
 */
public record ScenarioSessionLockProjection(
    Long scenarioId, UserScenarioProgressStatus progressStatus) {}
