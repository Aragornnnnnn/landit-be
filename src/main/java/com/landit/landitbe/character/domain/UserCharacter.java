// 사용자별 캐릭터 성장 상태를 저장한다.

package com.landit.landitbe.character.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 사용자별 캐릭터 성장 상태를 저장한다. */
@Entity
@Table(name = "user_character")
public class UserCharacter extends BaseTimeEntity {

  @Id
  @Column(name = "user_profile_id")
  private Long userProfileId;

  @Column(name = "character_stage_id", nullable = false)
  private Long characterStageId;

  @Column(name = "total_xp", nullable = false)
  private int totalXp;

  @Column(name = "last_stage_up_at")
  private LocalDateTime lastStageUpAt;

  /** 동작을 수행한다. */
  protected UserCharacter() {}
}
