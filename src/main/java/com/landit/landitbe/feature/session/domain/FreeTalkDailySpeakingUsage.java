// 프리톡의 사용자별 일일 발화 사용량을 저장한다.

package com.landit.landitbe.feature.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;

/** 프리톡의 사용자별 일일 발화 사용량을 저장한다. */
@Getter
@Entity
@Table(name = "free_talk_daily_speaking_usage")
public class FreeTalkDailySpeakingUsage {

  @EmbeddedId private DailyUsageId id;

  @Column(name = "used_speaking_duration_ms", nullable = false)
  private long usedSpeakingDurationMs;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkDailySpeakingUsage() {}

  private FreeTalkDailySpeakingUsage(long userProfileId, LocalDate usageDate, long usedDurationMs) {
    if (usedDurationMs < 0) {
      throw new IllegalArgumentException("사용자 발화 시간은 0 이상이어야 합니다.");
    }
    id = new DailyUsageId(userProfileId, usageDate);
    usedSpeakingDurationMs = usedDurationMs;
  }

  /** 새 일일 발화 사용량을 생성한다. */
  public static FreeTalkDailySpeakingUsage create(
      long userProfileId, LocalDate usageDate, long usedDurationMs) {
    return new FreeTalkDailySpeakingUsage(userProfileId, usageDate, usedDurationMs);
  }

  /** 새 사용자 발화를 현재 사용량에 더한다. */
  public void reserve(long utteranceDurationMs) {
    if (utteranceDurationMs < 0) {
      throw new IllegalArgumentException("사용자 발화 시간은 0 이상이어야 합니다.");
    }
    usedSpeakingDurationMs += utteranceDurationMs;
  }

  /** 복합 기본 키를 정의한다. */
  @Embeddable
  @Getter
  public static class DailyUsageId implements Serializable {

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "usage_date")
    private LocalDate usageDate;

    /** JPA에서 사용하는 기본 생성자다. */
    protected DailyUsageId() {}

    private DailyUsageId(long userProfileId, LocalDate usageDate) {
      this.userProfileId = userProfileId;
      this.usageDate = usageDate;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (!(object instanceof DailyUsageId dailyUsageId)) {
        return false;
      }
      return Objects.equals(userProfileId, dailyUsageId.userProfileId)
          && Objects.equals(usageDate, dailyUsageId.usageDate);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userProfileId, usageDate);
    }
  }
}
