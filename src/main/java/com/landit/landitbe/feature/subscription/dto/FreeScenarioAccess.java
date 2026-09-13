// 무료 기회가 연결된 시나리오와 세션을 전달한다.

package com.landit.landitbe.feature.subscription.dto;

/**
 * 무료 기회가 연결된 시나리오와 세션을 전달한다.
 *
 * @param sessionId 예약된 세션 ID
 * @param scenarioId 예약된 시나리오 ID
 */
public record FreeScenarioAccess(Long sessionId, Long scenarioId) {}
