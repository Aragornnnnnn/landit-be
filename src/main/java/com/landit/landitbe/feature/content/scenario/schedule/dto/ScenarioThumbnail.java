// 시나리오 썸네일 조회 결과 한 행을 담는 JPA projection record다.

package com.landit.landitbe.feature.content.scenario.schedule.dto;

/**
 * 시나리오 썸네일 조회 결과 한 행을 담는 JPA projection record다.
 *
 * @param scenarioId 시나리오 ID
 * @param thumbnailUrl 시나리오 썸네일 URL
 */
public record ScenarioThumbnail(Long scenarioId, String thumbnailUrl) {}
