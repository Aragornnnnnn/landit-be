// 계정에 남아 있는 할인 기회의 서버 기준 상태를 전달한다.

package com.landit.landitbe.feature.profile.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 유효한 할인 기회를 전달한다.
 *
 * @param remainingSeconds 서버 기준 남은 초. 소수 초는 올림한다
 * @param expiresAt 서울 시간대 할인 만료 시각
 * @param newUser 부여 시점에 가입 후 7일 미만이었는지
 */
@Schema(description = "진행 중인 5분 할인 기회")
public record DiscountOffer(
    @Schema(description = "서버 기준 남은 초. 소수 초 올림", example = "300") long remainingSeconds,
    @Schema(description = "서울 시간대 만료 시각", example = "2026-09-21T14:35:00") LocalDateTime expiresAt,
    @Schema(description = "부여 당시 가입 후 7일 미만이면 true") boolean newUser) {}
