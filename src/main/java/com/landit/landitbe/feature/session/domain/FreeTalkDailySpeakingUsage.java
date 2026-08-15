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

  /**
   * 새 일일 발화 사용량을 생성한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param usageDate 사용량을 집계할 KST 날짜
   * @param usedDurationMs 생성 시점의 누적 사용자 발화 시간 밀리초
   * @return 사용자와 날짜에 연결된 일일 발화 사용량
   * @throws IllegalArgumentException 누적 발화 시간이 음수일 때
   */
  public static FreeTalkDailySpeakingUsage create(
      long userProfileId, LocalDate usageDate, long usedDurationMs) {
    return new FreeTalkDailySpeakingUsage(userProfileId, usageDate, usedDurationMs);
  }

  /**
   * 새 사용자 발화를 현재 사용량에 더한다.
   *
   * @param utteranceDurationMs 추가할 사용자 발화 시간 밀리초
   * @throws IllegalArgumentException 발화 시간이 음수이거나 누적값이 long 범위를 넘을 때
   */
  public void reserve(long utteranceDurationMs) {
    if (utteranceDurationMs < 0) {
      throw new IllegalArgumentException("사용자 발화 시간은 0 이상이어야 합니다.");
    }
    try {
      usedSpeakingDurationMs = Math.addExact(usedSpeakingDurationMs, utteranceDurationMs);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("사용자 발화 시간이 허용 범위를 초과했습니다.", exception);
    }
  }

  /**
   * 실패한 AI 요청에서 예약한 사용자 발화 시간을 되돌린다.
   *
   * @param utteranceDurationMs 되돌릴 사용자 발화 시간 밀리초
   * @throws IllegalArgumentException 발화 시간이 음수이거나 현재 사용량보다 클 때
   */
  public void release(long utteranceDurationMs) {
    if (utteranceDurationMs < 0 || utteranceDurationMs > usedSpeakingDurationMs) {
      throw new IllegalArgumentException("되돌릴 사용자 발화 시간이 올바르지 않습니다.");
    }
    usedSpeakingDurationMs -= utteranceDurationMs;
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

    /**
     * 다른 일일 사용량 복합 키와 사용자·날짜가 같은지 비교한다.
     *
     * @param object 비교할 객체
     * @return 사용자 프로필 ID와 사용 날짜가 모두 같으면 {@code true}
     */
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

    /**
     * 사용자 프로필 ID와 사용 날짜를 기반으로 해시값을 계산한다.
     *
     * @return 복합 키의 해시값
     */
    @Override
    public int hashCode() {
      return Objects.hash(userProfileId, usageDate);
    }
  }
}
