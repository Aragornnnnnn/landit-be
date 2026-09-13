// 기능 간 조회 결과를 불변 값으로 전달한다.

package com.landit.landitbe.feature.learning.dto;

import java.time.LocalDateTime;

/**
 * 날짜별 시나리오 이력 조회에 필요한 최초 복습 권한 정보를 담는다.
 *
 * @param scenarioId 최초 완료한 시나리오 ID
 * @param grantedAt 최초 완료 시각
 */
public record ScenarioAccessHistory(Long scenarioId, LocalDateTime grantedAt) {}
