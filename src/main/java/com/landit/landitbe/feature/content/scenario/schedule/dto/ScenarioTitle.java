// 시나리오 제목 조회 결과 한 행을 담는 JPA projection record다.

package com.landit.landitbe.feature.content.scenario.schedule.dto;

/**
 * 시나리오 제목 조회 결과 한 행을 담는 JPA projection record다.
 *
 * @param scenarioId 시나리오 ID
 * @param title 요청한 언어 조합의 시나리오 제목
 */
public record ScenarioTitle(Long scenarioId, String title) {}
