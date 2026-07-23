// 시나리오 안에서 순서를 가지는 고정 질문 원형을 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 시나리오 안에서 순서를 가지는 고정 질문 원형을 저장한다. */
@Getter
@Entity
@Table(name = "scenario_question")
public class ScenarioQuestion extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "scenario_id", nullable = false)
  private Long scenarioId;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActiveStatus status;

  /** JPA에서 사용하는 기본 생성자다. */
  protected ScenarioQuestion() {}
}
