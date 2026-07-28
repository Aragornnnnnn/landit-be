// 서비스 날짜별 일일 시나리오 일정을 조회하고 저장한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.DailyScenarioSchedule;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 서비스 날짜별 일일 시나리오 일정을 조회하고 저장한다. */
public interface DailyScenarioScheduleRepository
    extends JpaRepository<DailyScenarioSchedule, Long> {

  /**
   * 서비스 날짜에 배정된 일일 시나리오 일정을 조회한다.
   *
   * @param serviceDate 서비스 날짜
   * @return 해당 날짜의 일정. 배정되지 않았으면 비어 있다.
   */
  Optional<DailyScenarioSchedule> findByServiceDate(LocalDate serviceDate);
}
