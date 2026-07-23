// 캐릭터 종류와 성장 단계 정보를 저장한다.

package com.landit.landitbe.feature.character.domain;

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

/** 캐릭터 종류와 성장 단계 정보를 저장한다. */
@Entity
@Table(name = "character_stage")
public class CharacterStage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String species;

  @Column(name = "stage_number", nullable = false)
  private int stageNumber;

  @Column(name = "required_total_xp", nullable = false)
  private int requiredTotalXp;

  @Column(name = "asset_url", length = 500)
  private String assetUrl;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActiveStatus status;

  /** 동작을 수행한다. */
  protected CharacterStage() {}
}
