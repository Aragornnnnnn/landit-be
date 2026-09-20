// 업무 간에 전달할 CurrentStreak 값을 정의한다.

package com.landit.landitbe.feature.character.dto;

import com.landit.landitbe.feature.character.domain.UserLearningActivitySummary;
import java.time.LocalDate;

/**
 * 현재 스트릭 조회 결과다.
 *
 * @param currentStreakDays 현재 유효 스트릭 일수
 * @param activeToday 오늘 정상 완료 여부
 * @param today 스트릭 계산에 사용한 KST 기준 오늘 날짜
 */
public record CurrentStreak(int currentStreakDays, boolean activeToday, LocalDate today) {

  /**
   * 저장된 마지막 활동일로 현재 유효 스트릭을 계산한다.
   *
   * @param summary 누적 활동 요약
   * @param today 서비스 기준 오늘
   * @return 현재 스트릭
   */
  public static CurrentStreak from(UserLearningActivitySummary summary, LocalDate today) {
    LocalDate lastActivityDate = summary.getLastActivityDate();
    if (lastActivityDate == null || lastActivityDate.isBefore(today.minusDays(1))) {
      return new CurrentStreak(0, false, today);
    }
    return new CurrentStreak(summary.getCurrentStreakDays(), lastActivityDate.equals(today), today);
  }
}
