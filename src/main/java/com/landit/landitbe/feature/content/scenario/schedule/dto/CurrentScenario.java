// 기능 간 조회 결과를 불변 값으로 전달한다.

package com.landit.landitbe.feature.content.scenario.schedule.dto;

import com.landit.landitbe.feature.content.scenario.schedule.domain.DailyScenarioType;

/**
 * 현재 제공 중인 시나리오의 식별자와 제공 유형을 담는다.
 *
 * @param scenarioId 현재 제공 중인 시나리오 ID
 * @param type 신규 또는 재도전 제공 유형
 */
public record CurrentScenario(Long scenarioId, DailyScenarioType type) {}
