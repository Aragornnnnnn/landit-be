// 현재 스트릭 조회 API 응답을 표현한다.

package com.landit.landitbe.feature.character.dto;

import com.landit.landitbe.feature.character.service.StreakService;
import java.time.LocalDate;

/**
 * 현재 스트릭 조회 API 응답을 표현한다.
 *
 * @param currentStreakDays 현재 유효 스트릭 일수
 * @param activeToday 오늘 정상 완료 여부
 * @param today 스트릭 계산에 사용한 KST 기준 오늘 날짜
 */
public record CurrentStreakResponse(int currentStreakDays, boolean activeToday, LocalDate today) {

  /**
   * Service 조회 결과를 API 응답으로 변환한다.
   *
   * @param currentStreak 현재 스트릭 조회 결과
   * @return 현재 스트릭 API 응답
   */
  public static CurrentStreakResponse from(StreakService.CurrentStreak currentStreak) {
    return new CurrentStreakResponse(
        currentStreak.currentStreakDays(), currentStreak.activeToday(), currentStreak.today());
  }
}
