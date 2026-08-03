// 사용자별 시나리오 캘린더 조회 응답을 조립한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.ScenarioCalendarType;
import com.landit.landitbe.feature.content.dto.ScenarioCalendarResponse;
import com.landit.landitbe.feature.content.dto.ScenarioCalendarResponse.CalendarDayResponse;
import com.landit.landitbe.feature.content.repository.ScenarioSequenceQueryRepository;
import com.landit.landitbe.feature.content.repository.projection.ScenarioThumbnailProjection;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService.DailyCompletion;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자별 시나리오 캘린더 조회 응답을 조립한다. */
@RequiredArgsConstructor
@Service
public class ScenarioCalendarService {

  private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final int WEEK_WINDOW_DAYS = 7;

  private final ScenarioSequenceQueryRepository scenarioSequenceQueryRepository;
  private final ScenarioProgressionService scenarioProgressionService;
  private final ScenarioAccessService scenarioAccessService;
  private final UserProfileService userProfileService;
  private final Clock clock;

  /**
   * 인증된 사용자의 시나리오 캘린더 응답을 조회한다. 기준 날짜가 포함된 창(WEEK 7칸, MONTH은 그 달 1일~말일)의 모든 칸을 날짜 오름차순으로 채워 반환한다.
   *
   * @param userId 인증된 사용자 ID
   * @param type 캘린더 조회 단위
   * @param date 창의 기준 날짜. null이면 서버 기준 오늘
   * @return 완료일 썸네일과 오늘 배정 시나리오가 반영된 캘린더 응답
   */
  @Transactional(readOnly = true)
  public ScenarioCalendarResponse getCalendar(
      long userId, ScenarioCalendarType type, LocalDate date) {
    Instant evaluatedAt = clock.instant();
    LocalDate today = evaluatedAt.atZone(SERVICE_ZONE_ID).toLocalDate();
    LocalDate baseDate = date != null ? date : today;

    LocalDate windowStart = windowStart(type, baseDate);
    int windowDays = windowDays(type, baseDate);
    CalendarWindow window = new CalendarWindow(windowStart, windowDays, today, evaluatedAt);
    // 창 마지막 날의 다음 날이다. granted_at이 시각이라 자정 경계 비교(< 다음 날 00:00)에 그대로 쓴다.
    LocalDate windowEndExclusive = windowStart.plusDays(windowDays);

    UserLocale userLocale = userProfileService.getUserLocale(userId);
    Map<LocalDate, Long> completedScenarioIdsByDate =
        completedScenarioIdsByDate(userId, userLocale, windowStart, windowEndExclusive);

    return ScenarioCalendarResponse.from(
        type,
        baseDate,
        label(type, baseDate),
        today,
        scenarioAccessService
            .findFirstCompletionDate(userId, userLocale.targetLocale())
            .orElse(null),
        calendarDays(userId, userLocale, window, completedScenarioIdsByDate));
  }

  /** 창의 시작 날짜를 계산한다. WEEK은 기준 날짜가 속한 주의 일요일, MONTH은 그 달 1일이다. */
  private static LocalDate windowStart(ScenarioCalendarType type, LocalDate baseDate) {
    if (type == ScenarioCalendarType.WEEK) {
      return baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    return baseDate.withDayOfMonth(1);
  }

  /** 창의 칸 수를 계산한다. WEEK은 7칸 고정, MONTH은 그 달의 실제 일수(28~31)다. */
  private static int windowDays(ScenarioCalendarType type, LocalDate baseDate) {
    return type == ScenarioCalendarType.WEEK ? WEEK_WINDOW_DAYS : baseDate.lengthOfMonth();
  }

  /** 기준 날짜가 속한 달을 기준으로 화면 헤더 문구를 만든다. 1일이 포함된 주(일요일 시작)가 1주차다. */
  private static String label(ScenarioCalendarType type, LocalDate baseDate) {
    if (type == ScenarioCalendarType.MONTH) {
      return "%d년 %d월".formatted(baseDate.getYear(), baseDate.getMonthValue());
    }

    LocalDate firstWeekStart =
        baseDate.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    LocalDate weekStart = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    long weekOfMonth = ChronoUnit.WEEKS.between(firstWeekStart, weekStart) + 1;

    return "%d년 %d월 %d주차".formatted(baseDate.getYear(), baseDate.getMonthValue(), weekOfMonth);
  }

  /** 창 구간의 날짜별 완료 시나리오 ID를 조회한다. */
  private Map<LocalDate, Long> completedScenarioIdsByDate(
      long userId, UserLocale userLocale, LocalDate windowStart, LocalDate windowEndExclusive) {
    List<DailyCompletion> completions =
        scenarioAccessService.findCompletionsBetween(
            userId, userLocale.targetLocale(), windowStart, windowEndExclusive);

    Map<LocalDate, Long> scenarioIdsByDate = new LinkedHashMap<>();
    for (DailyCompletion completion : completions) {
      scenarioIdsByDate.put(completion.date(), completion.scenarioId());
    }

    return scenarioIdsByDate;
  }

  /** 창의 모든 칸을 날짜 오름차순으로 채운다. 완료일은 썸네일과 함께, 미완료 오늘은 배정 시나리오 ID만 채운다. */
  private List<CalendarDayResponse> calendarDays(
      long userId,
      UserLocale userLocale,
      CalendarWindow window,
      Map<LocalDate, Long> completedScenarioIdsByDate) {
    Map<Long, String> thumbnailUrlsByScenarioId =
        thumbnailUrlsByScenarioId(completedScenarioIdsByDate.values().stream().toList());

    List<CalendarDayResponse> days = new ArrayList<>();
    for (int dayOffset = 0; dayOffset < window.days(); dayOffset++) {
      LocalDate cellDate = window.start().plusDays(dayOffset);

      Long completedScenarioId = completedScenarioIdsByDate.get(cellDate);
      if (completedScenarioId != null) {
        days.add(
            CalendarDayResponse.completedDay(
                cellDate, completedScenarioId, thumbnailUrlsByScenarioId.get(completedScenarioId)));
        continue;
      }

      if (cellDate.equals(window.today())) {
        days.add(
            CalendarDayResponse.uncompletedToday(
                cellDate, assignedScenarioId(userId, userLocale, window.evaluatedAt())));
        continue;
      }

      days.add(CalendarDayResponse.emptyDay(cellDate));
    }

    return List.copyOf(days);
  }

  /** 오늘 배정된 시나리오 ID를 조회한다. 모든 시나리오를 완료했으면 null이다. */
  private Long assignedScenarioId(long userId, UserLocale userLocale, Instant evaluatedAt) {
    return scenarioProgressionService
        .findCurrentScenario(userId, userLocale.targetLocale(), evaluatedAt)
        .map(ScenarioProgressionService.CurrentScenario::scenarioId)
        .orElse(null);
  }

  /** 완료한 시나리오의 썸네일 URL을 시나리오 ID로 조회한다. */
  private Map<Long, String> thumbnailUrlsByScenarioId(List<Long> scenarioIds) {
    if (scenarioIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> thumbnailUrls = new HashMap<>();
    for (ScenarioThumbnailProjection thumbnail :
        scenarioSequenceQueryRepository.findThumbnailsByScenarioIds(scenarioIds)) {
      thumbnailUrls.put(thumbnail.scenarioId(), thumbnail.thumbnailUrl());
    }

    return thumbnailUrls;
  }

  /** 한 요청에서 동일한 평가 시각을 사용하는 캘린더 창이다. */
  private record CalendarWindow(LocalDate start, int days, LocalDate today, Instant evaluatedAt) {}
}
