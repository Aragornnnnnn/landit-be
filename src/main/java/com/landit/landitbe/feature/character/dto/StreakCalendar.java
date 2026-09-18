// 업무 간에 전달할 StreakCalendar 값을 정의한다.

package com.landit.landitbe.feature.character.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 월별 스트릭 조회 결과다.
 *
 * @param yearMonth 조회한 연·월
 * @param currentStreakDays 현재 유효 스트릭 일수
 * @param activeToday 오늘 정상 완료 여부
 * @param today 스트릭 계산에 사용한 KST 기준 오늘 날짜
 * @param firstActiveDate 기능 출시 후 첫 완료일
 * @param longestStreakDays 최장 스트릭 일수
 * @param totalActiveDays 전체 활성 학습일 수
 * @param activeDates 요청한 월의 완료 날짜
 */
public record StreakCalendar(
    YearMonth yearMonth,
    int currentStreakDays,
    boolean activeToday,
    LocalDate today,
    LocalDate firstActiveDate,
    int longestStreakDays,
    int totalActiveDays,
    List<LocalDate> activeDates) {}
