// 시나리오 캘린더 조회 API의 응답 구조를 정의한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.domain.ScenarioCalendarType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 시나리오 캘린더 조회 API의 응답 구조를 정의한다.
 *
 * @param type 캘린더 조회 단위
 * @param date 창의 기준 날짜. 요청에서 생략했으면 오늘이 담긴다
 * @param label 화면 헤더 문구. WEEK은 "2026년 7월 5주차", MONTH은 "2026년 7월"
 * @param today 서버 기준 오늘 날짜
 * @param startedAt 사용자가 처음 시나리오를 완료한 날. 이력이 없으면 null
 * @param days 창의 모든 칸. WEEK 7개, MONTH은 그 달 일수(28~31개), 날짜 오름차순
 */
@Schema(description = "시나리오 캘린더 조회 응답")
public record ScenarioCalendarResponse(
    @Schema(description = "캘린더 조회 단위") ScenarioCalendarType type,
    @Schema(description = "창의 기준 날짜. 요청에서 생략했으면 오늘") LocalDate date,
    @Schema(description = "화면 헤더 문구", example = "2026년 7월 5주차") String label,
    @Schema(description = "서버 기준 오늘 날짜") LocalDate today,
    @Schema(description = "사용자가 처음 시나리오를 완료한 날. 이력이 없으면 null") LocalDate startedAt,
    @Schema(description = "창의 모든 칸. WEEK 7개, MONTH은 그 달 일수(28~31개), 날짜 오름차순")
        List<CalendarDayResponse> days) {

  /**
   * 창의 기준 정보와 날짜별 칸을 캘린더 응답으로 변환한다.
   *
   * @param type 캘린더 조회 단위
   * @param date 창의 기준 날짜
   * @param label 화면 헤더 문구
   * @param today 서버 기준 오늘 날짜
   * @param startedAt 사용자가 처음 시나리오를 완료한 날
   * @param days 창의 모든 칸
   * @return 시나리오 캘린더 조회 응답
   */
  public static ScenarioCalendarResponse from(
      ScenarioCalendarType type,
      LocalDate date,
      String label,
      LocalDate today,
      LocalDate startedAt,
      List<CalendarDayResponse> days) {
    return new ScenarioCalendarResponse(type, date, label, today, startedAt, days);
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param date 해당 칸의 날짜
   * @param completed 그 날짜에 시나리오를 완료했는지 여부
   * @param scenarioId 완료한 시나리오 ID. 미완료 오늘 칸은 배정된 시나리오 ID, 그 외에는 null
   * @param thumbnailUrl 완료한 시나리오의 썸네일 URL. 미완료 칸은 null
   */
  @Schema(description = "시나리오 캘린더 날짜 칸 응답")
  public record CalendarDayResponse(
      @Schema(description = "해당 칸의 날짜") LocalDate date,
      @Schema(description = "그 날짜에 시나리오를 완료했는지 여부") boolean completed,
      @Schema(description = "완료한 시나리오 ID. 미완료 오늘 칸은 배정된 시나리오 ID, 그 외에는 null") Long scenarioId,
      @Schema(description = "완료한 시나리오의 썸네일 URL. 미완료 칸은 null") String thumbnailUrl) {

    /**
     * 완료한 날짜 칸을 생성한다.
     *
     * @param date 완료한 날짜
     * @param scenarioId 완료한 시나리오 ID
     * @param thumbnailUrl 완료한 시나리오의 썸네일 URL
     * @return 완료 상태의 날짜 칸
     */
    public static CalendarDayResponse completedDay(
        LocalDate date, Long scenarioId, String thumbnailUrl) {
      return new CalendarDayResponse(date, true, scenarioId, thumbnailUrl);
    }

    /**
     * 아직 완료하지 않은 오늘 칸을 생성한다. 썸네일은 완료 전까지 공개하지 않는다.
     *
     * @param date 오늘 날짜
     * @param assignedScenarioId 오늘 배정된 시나리오 ID. 배정할 시나리오가 없으면 null
     * @return 미완료 상태의 오늘 칸
     */
    public static CalendarDayResponse uncompletedToday(LocalDate date, Long assignedScenarioId) {
      return new CalendarDayResponse(date, false, assignedScenarioId, null);
    }

    /**
     * 완료 기록이 없는 빈 칸을 생성한다.
     *
     * @param date 해당 칸의 날짜
     * @return 빈 날짜 칸
     */
    public static CalendarDayResponse emptyDay(LocalDate date) {
      return new CalendarDayResponse(date, false, null, null);
    }
  }
}
