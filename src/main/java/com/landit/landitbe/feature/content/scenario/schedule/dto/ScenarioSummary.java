// 기능 간 조회 결과를 불변 값으로 전달한다.

package com.landit.landitbe.feature.content.scenario.schedule.dto;

/**
 * 사용자 상세에 제공할 시나리오 기본 정보다.
 *
 * @param scenarioId 시나리오 ID
 * @param scenarioTitle 시나리오 제목
 * @param displayOrder 시나리오 노출 순서
 */
public record ScenarioSummary(Long scenarioId, String scenarioTitle, int displayOrder) {}
