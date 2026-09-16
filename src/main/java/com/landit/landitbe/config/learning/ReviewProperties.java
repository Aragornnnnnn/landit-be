// 복습의 발송 간격과 시작·재개 유효기간을 설정한다.

package com.landit.landitbe.config.learning;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 복습 기본 정책을 설정한다.
 *
 * @param intervalDays 복습 알림 간격
 * @param exclusionDays 최근 학습·복습·출제 표현의 제외 기간
 * @param availableDays 생성 후 시작할 수 있는 기간
 * @param sessionHours 시작 후 재개할 수 있는 시간
 * @param notificationGapHours 학습 알림 사이 최소 간격
 */
@Validated
@ConfigurationProperties("landit.review")
public record ReviewProperties(
    @DefaultValue("3") @Min(1) int intervalDays,
    @DefaultValue("3") @Min(1) int exclusionDays,
    @DefaultValue("7") @Min(1) int availableDays,
    @DefaultValue("24") @Min(1) int sessionHours,
    @DefaultValue("3") @Min(1) int notificationGapHours) {}
