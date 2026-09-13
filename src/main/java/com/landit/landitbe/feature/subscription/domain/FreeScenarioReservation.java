// 사용자별 첫 무료 기회를 하나의 시나리오 세션에 고정한다.

package com.landit.landitbe.feature.subscription.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

/** 중도 종료와 재접속으로 새 무료 기회를 만들지 않는 예약이다. */
@Entity
@Getter
@Table(name = "free_scenario_reservation")
public class FreeScenarioReservation {
  @Id private Long userId;
  private Long sessionId;
  private Long scenarioId;
  private LocalDateTime reservedAt;

  /** JPA용 생성자다. */
  protected FreeScenarioReservation() {}

  /** 사용자 잠금 아래 첫 무료 시작을 기록한다. */
  public FreeScenarioReservation(long userId, long sessionId, long scenarioId, LocalDateTime now) {
    this.userId = userId;
    this.sessionId = sessionId;
    this.scenarioId = scenarioId;
    this.reservedAt = now;
  }
}
