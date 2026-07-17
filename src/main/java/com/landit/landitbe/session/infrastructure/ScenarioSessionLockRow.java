// 시나리오 순차 잠금 계산에 필요한 최소 조회 결과를 담는다.

package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.learning.domain.UserScenarioProgressStatus;

/** 시나리오 순차 잠금 계산에 필요한 최소 조회 결과를 담는다. */
public record ScenarioSessionLockRow(Long scenarioId, UserScenarioProgressStatus progressStatus) {}
