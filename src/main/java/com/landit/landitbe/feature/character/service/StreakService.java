// 사용자별 스트릭 활동을 기록하고 현재·월별 스트릭 정보를 조회한다.

package com.landit.landitbe.feature.character.service;

import com.landit.landitbe.feature.character.domain.UserDailyActivity;
import com.landit.landitbe.feature.character.domain.UserLearningActivitySummary;
import com.landit.landitbe.feature.character.repository.UserDailyActivityRepository;
import com.landit.landitbe.feature.character.repository.UserLearningActivitySummaryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자별 스트릭 활동을 기록하고 현재·월별 스트릭 정보를 조회한다. */
@RequiredArgsConstructor
@Service
public class StreakService {

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final UserDailyActivityRepository userDailyActivityRepository;
  private final UserLearningActivitySummaryRepository summaryRepository;

  /**
   * 정상 완료한 대화를 해당 날짜의 스트릭 활동으로 기록한다.
   *
   * <p>호출자는 같은 트랜잭션에서 학습 세션 잠금보다 먼저 사용자 프로필 쓰기 잠금을 획득해야 한다.
   *
   * <p>이 잠금 순서로 같은 사용자의 완료 기록을 직렬화한다.
   *
   * @param userId 사용자 ID
   * @param completedAt Asia/Seoul 기준 세션 완료 시각
   */
  @Transactional
  public void recordCompletedConversation(long userId, LocalDateTime completedAt) {
    LocalDate activityDate = completedAt.toLocalDate();
    var existingActivity =
        userDailyActivityRepository.findByUserProfileIdAndActivityDate(userId, activityDate);
    if (existingActivity.isPresent()) {
      UserDailyActivity dailyActivity = existingActivity.get();
      boolean alreadyActive = dailyActivity.isActiveDay();
      dailyActivity.completeSession();
      if (alreadyActive) {
        return;
      }
    } else {
      userDailyActivityRepository.save(UserDailyActivity.startActiveDay(userId, activityDate));
    }

    UserLearningActivitySummary summary =
        summaryRepository
            .findById(userId)
            .orElseGet(
                () -> {
                  UserLearningActivitySummary initialized =
                      UserLearningActivitySummary.initialize(userId);
                  return summaryRepository.save(initialized);
                });
    summary.recordActiveDay(activityDate);
  }

  /**
   * 사용자의 현재 유효 스트릭과 오늘 완료 여부를 조회한다.
   *
   * @param userId 사용자 ID
   * @return 현재 스트릭 정보
   */
  @Transactional(readOnly = true)
  public CurrentStreak getCurrentStreak(long userId) {
    LocalDate today = LocalDate.now(KOREA_ZONE_ID);
    return summaryRepository
        .findById(userId)
        .map(summary -> CurrentStreak.from(summary, today))
        .orElseGet(() -> new CurrentStreak(0, false));
  }

  /**
   * 사용자의 전체 스트릭과 지정한 월의 완료 날짜를 조회한다.
   *
   * @param userId 사용자 ID
   * @param yearMonth 조회할 연·월
   * @return 월별 스트릭 정보
   */
  @Transactional(readOnly = true)
  public StreakCalendar getCalendar(long userId, YearMonth yearMonth) {
    LocalDate monthStart = yearMonth.atDay(1);
    LocalDate nextMonthStart = yearMonth.plusMonths(1).atDay(1);
    CurrentStreak currentStreak = getCurrentStreak(userId);
    UserLearningActivitySummary summary = summaryRepository.findById(userId).orElse(null);
    List<LocalDate> activeDates =
        userDailyActivityRepository
            .findAllActiveInDateRange(userId, monthStart, nextMonthStart)
            .stream()
            .map(UserDailyActivity::getActivityDate)
            .toList();
    LocalDate streakStartedDate =
        userDailyActivityRepository
            .findFirstByUserProfileIdAndActiveDayTrueOrderByActivityDateAsc(userId)
            .map(UserDailyActivity::getActivityDate)
            .orElse(null);
    int longestStreakDays = summary == null ? 0 : summary.getLongestStreakDays();
    int totalActiveDays =
        Math.toIntExact(userDailyActivityRepository.countByUserProfileIdAndActiveDayTrue(userId));

    return new StreakCalendar(
        currentStreak.currentStreakDays(),
        currentStreak.activeToday(),
        streakStartedDate,
        longestStreakDays,
        totalActiveDays,
        activeDates);
  }

  /**
   * 현재 스트릭 조회 결과다.
   *
   * @param currentStreakDays 현재 유효 스트릭 일수
   * @param activeToday 오늘 정상 완료 여부
   */
  public record CurrentStreak(int currentStreakDays, boolean activeToday) {

    private static CurrentStreak from(UserLearningActivitySummary summary, LocalDate today) {
      LocalDate lastActivityDate = summary.getLastActivityDate();
      if (lastActivityDate == null || lastActivityDate.isBefore(today.minusDays(1))) {
        return new CurrentStreak(0, false);
      }
      return new CurrentStreak(summary.getCurrentStreakDays(), lastActivityDate.equals(today));
    }
  }

  /**
   * 월별 스트릭 조회 결과다.
   *
   * @param currentStreakDays 현재 유효 스트릭 일수
   * @param activeToday 오늘 정상 완료 여부
   * @param streakStartedDate 기능 출시 후 첫 완료일
   * @param longestStreakDays 최장 스트릭 일수
   * @param totalActiveDays 전체 활성 학습일 수
   * @param activeDates 요청한 월의 완료 날짜
   */
  public record StreakCalendar(
      int currentStreakDays,
      boolean activeToday,
      LocalDate streakStartedDate,
      int longestStreakDays,
      int totalActiveDays,
      List<LocalDate> activeDates) {}
}
