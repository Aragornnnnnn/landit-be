// 기능 간 조회 결과를 불변 값으로 전달한다.

package com.landit.landitbe.feature.learning.scenario.access.dto;

import java.time.LocalDate;

/**
 * 사용자가 하루 동안 완료한 시나리오를 담는다.
 *
 * @param date 완료한 날짜
 * @param scenarioId 완료한 시나리오 ID
 */
public record DailyCompletion(LocalDate date, Long scenarioId) {}
