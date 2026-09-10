// 사용자별 일일 알람의 시각과 활성 상태 변경 요청을 검증한다.

package com.landit.landitbe.feature.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 사용자별 일일 알람 설정 전체를 변경하는 요청이다.
 *
 * @param time 기기 현지 시간 기준 알람 시각. 00:00부터 23:59까지의 HH:mm 형식
 * @param enabled 알람 활성화 여부
 */
@Schema(description = "사용자 일일 알람 설정 변경 요청")
public record UserAlarmUpdateRequest(
    @NotNull
        @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
        @Schema(description = "기기 현지 시간 기준 시각. 비활성화할 때도 전달합니다.", example = "07:30")
        String time,
    @NotNull @Schema(description = "알람 활성화 여부", example = "true") Boolean enabled) {}
