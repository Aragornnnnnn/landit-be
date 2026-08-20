// 사용자 언어 설정에 맞는 시나리오 기본 정보 조회 결과를 전달한다.

package com.landit.landitbe.feature.content.repository.projection;

/**
 * 사용자 언어 설정에 맞는 시나리오 기본 정보 조회 결과다.
 *
 * @param scenarioId 시나리오 ID
 * @param scenarioTitle 시나리오 제목
 * @param displayOrder 시나리오 노출 순서
 */
public record ScenarioSummaryProjection(Long scenarioId, String scenarioTitle, int displayOrder) {}
