// 서울 서비스 날짜를 기준으로 일일 시나리오 일정과 다음 갱신 시각을 계산한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.DailyScenarioSchedule;
import com.landit.landitbe.feature.content.repository.DailyScenarioScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 서울 서비스 날짜를 기준으로 일일 시나리오 일정과 다음 갱신 시각을 계산한다. */
@Service
@RequiredArgsConstructor
public class DailyScenarioScheduleService {

  private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final DailyScenarioScheduleRepository dailyScenarioScheduleRepository;
  private final Clock clock;

  /**
   * 현재 서울 서비스 날짜의 일정과 다음 서비스 날짜가 시작되는 시각을 조회한다.
   *
   * @return 오늘 일정과 다음 날 자정 시각
   */
  @Transactional(readOnly = true)
  public TodaySchedule findTodaySchedule() {
    Instant evaluatedAt = clock.instant();
    LocalDate serviceDate = evaluatedAt.atZone(SERVICE_ZONE_ID).toLocalDate();
    Instant nextDayStart = serviceDate.plusDays(1).atStartOfDay(SERVICE_ZONE_ID).toInstant();
    Optional<DailyScenarioSchedule> schedule =
        dailyScenarioScheduleRepository.findByServiceDate(serviceDate);
    return new TodaySchedule(schedule, nextDayStart);
  }

  /**
   * 서울 서비스 날짜의 일정과 다음 날 자정 시각을 함께 반환한다.
   *
   * @param schedule 오늘 배정된 일정
   * @param nextDayStart 다음 서울 서비스 날짜 시작 시각
   */
  public record TodaySchedule(Optional<DailyScenarioSchedule> schedule, Instant nextDayStart) {}
}
