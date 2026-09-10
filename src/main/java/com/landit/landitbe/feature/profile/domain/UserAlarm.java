// 클라이언트에서 매일 반복 예약할 사용자별 알람 시각과 활성 상태를 저장한다.

package com.landit.landitbe.feature.profile.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;

/** 클라이언트에서 매일 반복 예약할 사용자별 알람 설정이다. */
@Getter
@Entity
@Table(name = "user_alarm")
public class UserAlarm extends BaseTimeEntity {

  @Id
  @Column(name = "user_profile_id")
  private Long userProfileId;

  @Column(name = "alarm_time", nullable = false)
  private LocalTime alarmTime;

  @Column(nullable = false)
  private boolean enabled;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserAlarm() {}

  /**
   * 사용자에게 속한 하나의 일일 알람 설정을 생성한다.
   *
   * @param userProfileId 알람 소유자 ID
   * @param alarmTime 기기 현지 시간 기준 분 단위 알람 시각
   * @param enabled 알람 활성화 여부
   */
  public UserAlarm(Long userProfileId, LocalTime alarmTime, boolean enabled) {
    this.userProfileId = userProfileId;
    update(alarmTime, enabled);
  }

  /**
   * 알람 시각과 활성 상태를 함께 변경한다.
   *
   * @param alarmTime 기기 현지 시간 기준 분 단위 알람 시각
   * @param enabled 알람 활성화 여부
   */
  public void update(LocalTime alarmTime, boolean enabled) {
    this.alarmTime = alarmTime;
    this.enabled = enabled;
  }
}
