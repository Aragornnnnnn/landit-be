// 기능 간 조회 결과를 불변 값으로 전달한다.

package com.landit.landitbe.feature.character.dto;

import java.time.LocalDate;

/**
 * 관리자 사용자 상세에 제공할 학습 활동 요약이다.
 *
 * @param currentStreakDays 현재 스트릭 일수
 * @param lastActivityDate 마지막 학습일
 */
public record LearningActivitySummary(int currentStreakDays, LocalDate lastActivityDate) {}
