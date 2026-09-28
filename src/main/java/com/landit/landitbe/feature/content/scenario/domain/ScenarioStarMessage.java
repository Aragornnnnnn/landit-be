// 시나리오 최종 피드백에서 별점 구간별로 보여줄 고정 문구를 저장한다.

package com.landit.landitbe.feature.content.scenario.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;

/** 시나리오 최종 피드백에서 별점 구간별로 보여줄 고정 문구를 저장한다. */
@Getter
@Entity
@Table(name = "scenario_star_message")
public class ScenarioStarMessage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 예: 1

  @Column(name = "scenario_id", nullable = false)
  private Long scenarioId; // 예: 17

  // 예: 2.5 (1.0, 1.5, 2.0, 2.5, 3.0만 사용)
  @Column(name = "star_rating", nullable = false, precision = 2, scale = 1)
  private BigDecimal starRating;

  // 예: 이제 해외 카페에서 취향대로 주문할 수 있어요!
  @Column(nullable = false, columnDefinition = "text")
  private String message;

  /** JPA에서 사용하는 기본 생성자다. */
  protected ScenarioStarMessage() {}
}
