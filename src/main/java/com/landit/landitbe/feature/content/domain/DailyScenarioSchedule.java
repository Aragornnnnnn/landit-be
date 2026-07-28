// 서비스 날짜별로 모든 사용자에게 공통으로 제공할 시나리오를 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;

/** 서비스 날짜별로 모든 사용자에게 공통으로 제공할 시나리오를 저장한다. */
@Getter
@Entity
@Table(name = "daily_scenario_schedule")
public class DailyScenarioSchedule extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "service_date", nullable = false, unique = true)
  private LocalDate serviceDate;

  @Column(name = "scenario_id", nullable = false)
  private Long scenarioId;

  /** JPA에서 사용하는 기본 생성자다. */
  protected DailyScenarioSchedule() {}

  private DailyScenarioSchedule(LocalDate serviceDate, Long scenarioId) {
    this.serviceDate = serviceDate;
    this.scenarioId = scenarioId;
  }

  /**
   * 서비스 날짜에 배정할 시나리오 일정을 생성한다.
   *
   * @param serviceDate 서비스 날짜
   * @param scenarioId 배정할 시나리오 ID
   * @return 생성한 일일 시나리오 일정
   */
  public static DailyScenarioSchedule schedule(LocalDate serviceDate, Long scenarioId) {
    return new DailyScenarioSchedule(serviceDate, scenarioId);
  }
}
