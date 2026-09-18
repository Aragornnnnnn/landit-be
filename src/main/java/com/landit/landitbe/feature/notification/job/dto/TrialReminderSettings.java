// 운영자가 변경하는 체험 알림 채널 설정을 전달한다.

package com.landit.landitbe.feature.notification.job.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 발송 직전에 읽는 채널 설정이다.
 *
 * @param pushEnabled 체험 푸시 발송 여부
 * @param emailEnabled 체험 이메일 발송 여부
 */
public record TrialReminderSettings(@NotNull Boolean pushEnabled, @NotNull Boolean emailEnabled) {}
