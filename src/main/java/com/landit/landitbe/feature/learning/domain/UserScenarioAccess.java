// 사용자가 완료로 획득한 시나리오 복습 권한을 저장한다.

package com.landit.landitbe.feature.learning.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import com.landit.landitbe.shared.domain.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/** 사용자가 완료로 획득한 시나리오 복습 권한을 저장한다. */
@Getter
@Entity
@Table(
    name = "user_scenario_access",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_scenario_access_user_scenario_locale",
            columnNames = {"user_profile_id", "scenario_id", "target_locale"}))
public class UserScenarioAccess extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "scenario_id", nullable = false)
  private Long scenarioId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_locale", nullable = false, length = 35)
  private Locale targetLocale;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserScenarioAccess() {}

  private UserScenarioAccess(Long userProfileId, Long scenarioId, Locale targetLocale) {
    this.userProfileId = userProfileId;
    this.scenarioId = scenarioId;
    this.targetLocale = targetLocale;
  }

  /**
   * 시나리오 완료로 획득한 복습 권한을 생성한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 완료한 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @return 생성한 시나리오 복습 권한
   */
  public static UserScenarioAccess grant(Long userProfileId, Long scenarioId, Locale targetLocale) {
    return new UserScenarioAccess(userProfileId, scenarioId, targetLocale);
  }
}
