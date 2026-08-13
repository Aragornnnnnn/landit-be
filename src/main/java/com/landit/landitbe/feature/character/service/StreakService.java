// 사용자별 스트릭 활동을 기록하고 현재·월별 스트릭 정보를 조회한다.

package com.landit.landitbe.feature.character.service;

import com.landit.landitbe.feature.character.domain.UserDailyActivity;
import com.landit.landitbe.feature.character.domain.UserLearningActivitySummary;
import com.landit.landitbe.feature.character.repository.UserDailyActivityRepository;
import com.landit.landitbe.feature.character.repository.UserLearningActivitySummaryRepository;
import java.time.Clock;
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
  private final Clock clock;

  /**
   * 정상 완료한 대화를 해당 날짜의 스트릭 활동으로 기록한다.
   *
   * <p>호출자는 같은 트랜잭션에서 사용자 프로필 쓰기 잠금을 먼저 획득해야 한다.
   *
   * <p>학습 세션보다 먼저 프로필을 잠가 같은 사용자의 완료 기록을 직렬화한다.
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
    LocalDate today = clock.instant().atZone(KOREA_ZONE_ID).toLocalDate();
    return currentStreak(userId, today);
  }

  /**
   * 사용자의 현재 스트릭과 마지막 학습일을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 현재 스트릭과 마지막 학습일
   */
  @Transactional(readOnly = true)
  public LearningActivitySummary getLearningActivitySummary(long userId) {
    LocalDate today = clock.instant().atZone(KOREA_ZONE_ID).toLocalDate();
    return summaryRepository
        .findById(userId)
        .map(
            summary -> {
              CurrentStreak currentStreak = CurrentStreak.from(summary, today);
              return new LearningActivitySummary(
                  currentStreak.currentStreakDays(), summary.getLastActivityDate());
            })
        .orElseGet(() -> new LearningActivitySummary(0, null));
  }

  private CurrentStreak currentStreak(long userId, LocalDate today) {
    return summaryRepository
        .findById(userId)
        .map(summary -> CurrentStreak.from(summary, today))
        .orElseGet(() -> new CurrentStreak(0, false, today));
  }

  /**
   * 사용자의 전체 스트릭과 지정한 월의 완료 날짜를 조회한다.
   *
   * @param userId 사용자 ID
   * @param requestedMonth 조회할 연·월. null이면 KST 오늘이 속한 월
   * @return 월별 스트릭 정보
   */
  @Transactional(readOnly = true)
  public StreakCalendar getCalendar(long userId, YearMonth requestedMonth) {
    LocalDate today = clock.instant().atZone(KOREA_ZONE_ID).toLocalDate();
    YearMonth yearMonth = requestedMonth != null ? requestedMonth : YearMonth.from(today);
    LocalDate monthStart = yearMonth.atDay(1);
    LocalDate nextMonthStart = yearMonth.plusMonths(1).atDay(1);
    CurrentStreak currentStreak = currentStreak(userId, today);
    UserLearningActivitySummary summary = summaryRepository.findById(userId).orElse(null);
    List<LocalDate> activeDates =
        userDailyActivityRepository
            .findAllActiveInDateRange(userId, monthStart, nextMonthStart)
            .stream()
            .map(UserDailyActivity::getActivityDate)
            .toList();
    LocalDate firstActiveDate =
        userDailyActivityRepository
            .findFirstByUserProfileIdAndActiveDayTrueOrderByActivityDateAsc(userId)
            .map(UserDailyActivity::getActivityDate)
            .orElse(null);
    int longestStreakDays = summary == null ? 0 : summary.getLongestStreakDays();
    int totalActiveDays =
        Math.toIntExact(userDailyActivityRepository.countByUserProfileIdAndActiveDayTrue(userId));

    return new StreakCalendar(
        yearMonth,
        currentStreak.currentStreakDays(),
        currentStreak.activeToday(),
        currentStreak.today(),
        firstActiveDate,
        longestStreakDays,
        totalActiveDays,
        activeDates);
  }

  /**
   * 현재 스트릭 조회 결과다.
   *
   * @param currentStreakDays 현재 유효 스트릭 일수
   * @param activeToday 오늘 정상 완료 여부
   * @param today 스트릭 계산에 사용한 KST 기준 오늘 날짜
   */
  public record CurrentStreak(int currentStreakDays, boolean activeToday, LocalDate today) {

    // 저장된 마지막 활동일을 기준으로 현재 유효 스트릭을 계산한다.
    private static CurrentStreak from(UserLearningActivitySummary summary, LocalDate today) {
      LocalDate lastActivityDate = summary.getLastActivityDate();
      if (lastActivityDate == null || lastActivityDate.isBefore(today.minusDays(1))) {
        return new CurrentStreak(0, false, today);
      }
      return new CurrentStreak(
          summary.getCurrentStreakDays(), lastActivityDate.equals(today), today);
    }
  }

  /**
   * 관리자 사용자 상세에 제공할 학습 활동 요약이다.
   *
   * @param currentStreakDays 현재 스트릭 일수
   * @param lastActivityDate 마지막 학습일
   */
  public record LearningActivitySummary(int currentStreakDays, LocalDate lastActivityDate) {}

  /**
   * 월별 스트릭 조회 결과다.
   *
   * @param yearMonth 조회한 연·월
   * @param currentStreakDays 현재 유효 스트릭 일수
   * @param activeToday 오늘 정상 완료 여부
   * @param today 스트릭 계산에 사용한 KST 기준 오늘 날짜
   * @param firstActiveDate 기능 출시 후 첫 완료일
   * @param longestStreakDays 최장 스트릭 일수
   * @param totalActiveDays 전체 활성 학습일 수
   * @param activeDates 요청한 월의 완료 날짜
   */
  public record StreakCalendar(
      YearMonth yearMonth,
      int currentStreakDays,
      boolean activeToday,
      LocalDate today,
      LocalDate firstActiveDate,
      int longestStreakDays,
      int totalActiveDays,
      List<LocalDate> activeDates) {}
}
