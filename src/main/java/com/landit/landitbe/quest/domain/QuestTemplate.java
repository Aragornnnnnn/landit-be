// 여러 사용자에게 재사용 가능한 퀘스트 원형을 저장한다.
package com.landit.landitbe.quest.domain;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quest_template")
public class QuestTemplate extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "quest_type", nullable = false, length = 20)
  private QuestType questType;

  @Column(name = "target_count", nullable = false)
  private int targetCount;

  @Column(name = "scenario_id")
  private Long scenarioId;

  @Column(name = "xp_reward", nullable = false)
  private int xpReward;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActiveStatus status;

  protected QuestTemplate() {}
}
