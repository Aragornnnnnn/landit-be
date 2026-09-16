// 무료 체험 종료 알림의 상품 범위와 예약 시간을 설정한다.

package com.landit.landitbe.config.notification;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 체험 종료 알림 정책이다.
 *
 * @param annualProductIds 정확히 일치해야 하는 연간 상품 ID
 * @param leadTime 체험 종료보다 앞선 발송 간격
 * @param maxLateness 발송 예정 시각 이후 허용할 지연
 * @param schedulingEnabled 체험 예약 및 발송 허용 여부
 * @param sandboxEnabled SANDBOX 체험 예약 허용 여부
 */
@ConfigurationProperties("landit.notification.trial-reminder")
public record TrialReminderProperties(
    Set<String> annualProductIds,
    Duration leadTime,
    Duration maxLateness,
    boolean schedulingEnabled,
    boolean sandboxEnabled) {
  /** 누락된 시간 설정은 24시간 전과 최대 2시간 지연으로 설정한다. */
  public TrialReminderProperties {
    annualProductIds =
        annualProductIds == null
            ? Set.of()
            : annualProductIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    leadTime = leadTime == null ? Duration.ofHours(24) : leadTime;
    maxLateness = maxLateness == null ? Duration.ofHours(2) : maxLateness;
    if (leadTime.isNegative()
        || leadTime.isZero()
        || maxLateness.isNegative()
        || maxLateness.compareTo(leadTime) >= 0) {
      throw new IllegalArgumentException("체험 알림 시간 설정이 올바르지 않습니다.");
    }
  }
}
