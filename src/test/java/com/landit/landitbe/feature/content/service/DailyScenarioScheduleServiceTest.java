// 서울 서비스 날짜 기준으로 일일 시나리오 일정 조회 시점을 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.domain.DailyScenarioSchedule;
import com.landit.landitbe.feature.content.repository.DailyScenarioScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 서울 자정 경계에서 일일 시나리오 일정과 다음 갱신 시각을 검증한다. */
@ExtendWith(MockitoExtension.class)
class DailyScenarioScheduleServiceTest {

  @Mock private DailyScenarioScheduleRepository dailyScenarioScheduleRepository;

  @Test
  void findsTodaysScheduleAndNextMidnightFromSeoulTime() {
    DailyScenarioSchedule schedule =
        DailyScenarioSchedule.schedule(LocalDate.of(2026, 7, 28), 100L);
    when(dailyScenarioScheduleRepository.findByServiceDate(LocalDate.of(2026, 7, 28)))
        .thenReturn(Optional.of(schedule));
    DailyScenarioScheduleService service =
        new DailyScenarioScheduleService(
            dailyScenarioScheduleRepository,
            Clock.fixed(Instant.parse("2026-07-28T14:59:59Z"), ZoneOffset.UTC));

    DailyScenarioScheduleService.TodaySchedule result = service.findTodaySchedule();

    assertThat(result.schedule()).containsSame(schedule);
    assertThat(result.nextDayStart()).isEqualTo(Instant.parse("2026-07-28T15:00:00Z"));
  }

  @Test
  void changesTodayAtSeoulMidnight() {
    DailyScenarioSchedule schedule =
        DailyScenarioSchedule.schedule(LocalDate.of(2026, 7, 29), 101L);
    when(dailyScenarioScheduleRepository.findByServiceDate(LocalDate.of(2026, 7, 29)))
        .thenReturn(Optional.of(schedule));
    DailyScenarioScheduleService service =
        new DailyScenarioScheduleService(
            dailyScenarioScheduleRepository,
            Clock.fixed(Instant.parse("2026-07-28T15:00:00Z"), ZoneOffset.UTC));

    DailyScenarioScheduleService.TodaySchedule result = service.findTodaySchedule();

    assertThat(result.schedule()).containsSame(schedule);
    assertThat(result.nextDayStart()).isEqualTo(Instant.parse("2026-07-29T15:00:00Z"));
  }
}
