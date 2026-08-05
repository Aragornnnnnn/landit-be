// 월별 스트릭 달력 조회 API 응답을 표현한다.

package com.landit.landitbe.feature.character.dto;

import com.landit.landitbe.feature.character.service.StreakService;
import java.time.LocalDate;
import java.util.List;

/**
 * 월별 스트릭 달력 조회 API 응답을 표현한다.
 *
 * @param year 조회한 연도
 * @param month 조회한 월
 * @param today 스트릭 계산에 사용한 KST 기준 오늘 날짜
 * @param currentStreakDays 현재 유효 스트릭 일수
 * @param activeToday 오늘 정상 완료 여부
 * @param firstActiveDate 기능 출시 후 첫 완료일
 * @param longestStreakDays 최장 스트릭 일수
 * @param totalActiveDays 전체 활성 학습일 수
 * @param activeDates 요청한 월의 완료 날짜
 */
public record StreakCalendarResponse(
    int year,
    int month,
    LocalDate today,
    int currentStreakDays,
    boolean activeToday,
    LocalDate firstActiveDate,
    int longestStreakDays,
    int totalActiveDays,
    List<LocalDate> activeDates) {

  /**
   * Service 조회 결과를 API 응답으로 변환한다.
   *
   * @param year 조회한 연도
   * @param month 조회한 월
   * @param calendar 월별 스트릭 조회 결과
   * @return 월별 스트릭 달력 API 응답
   */
  public static StreakCalendarResponse from(
      int year, int month, StreakService.StreakCalendar calendar) {
    return new StreakCalendarResponse(
        year,
        month,
        calendar.today(),
        calendar.currentStreakDays(),
        calendar.activeToday(),
        calendar.firstActiveDate(),
        calendar.longestStreakDays(),
        calendar.totalActiveDays(),
        calendar.activeDates());
  }
}
