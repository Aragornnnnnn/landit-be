// 한국 시간의 캠페인 예약 입력을 정의한다.

package com.landit.landitbe.feature.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * 한국 시간으로 지정하는 일회성 예약이다.
 *
 * @param scheduledAt +09:00 오프셋, 초 단위의 예약 시각. 최초 요청은 1분 이후
 */
public record AdminPushScheduleRequest(
    @NotNull @Schema(example = "2026-09-10T19:00:00+09:00") OffsetDateTime scheduledAt) {}
