// 업무 간에 전달할 DailySpeakingUsage 값을 정의한다.

package com.landit.landitbe.feature.session.freetalk.usage.dto;

import java.time.LocalDate;

/**
 * 예약 후 일일 누적 발화 시간과 남은 시간을 반환한다.
 *
 * @param usageDate 사용량을 집계한 KST 날짜
 * @param usedSpeakingDurationMs KST 당일 사용한 사용자 발화 시간 밀리초
 * @param remainingMs KST 당일 남은 사용자 발화 시간 밀리초
 */
public record DailySpeakingUsage(
    LocalDate usageDate, long usedSpeakingDurationMs, long remainingMs) {}
