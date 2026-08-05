// StreakService의 날짜별 활동 기록과 스트릭 계산을 검증한다.

package com.landit.landitbe.feature.character.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.character.domain.UserDailyActivity;
import com.landit.landitbe.feature.character.domain.UserLearningActivitySummary;
import com.landit.landitbe.feature.character.repository.UserDailyActivityRepository;
import com.landit.landitbe.feature.character.repository.UserLearningActivitySummaryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** StreakService의 날짜별 활동 기록과 스트릭 계산을 검증한다. */
@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

  private static final long USER_ID = 1L;
  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Mock private UserDailyActivityRepository userDailyActivityRepository;

  @Mock private UserLearningActivitySummaryRepository summaryRepository;

  @InjectMocks private StreakService streakService;

  /** 첫 정상 완료는 활동일과 현재·최장 스트릭을 1로 만든다. */
  @Test
  void createsFirstActiveDayAndStreak() {
    LocalDate activityDate = LocalDate.now(KOREA_ZONE_ID);
    when(userDailyActivityRepository.findByUserProfileIdAndActivityDate(USER_ID, activityDate))
        .thenReturn(Optional.empty());
    when(summaryRepository.findById(USER_ID)).thenReturn(Optional.empty());
    UserLearningActivitySummary persistedSummary = UserLearningActivitySummary.initialize(USER_ID);
    when(summaryRepository.save(any())).thenReturn(persistedSummary);

    streakService.recordCompletedConversation(USER_ID, activityDate.atTime(12, 0));

    ArgumentCaptor<UserDailyActivity> dailyActivityCaptor =
        ArgumentCaptor.forClass(UserDailyActivity.class);
    ArgumentCaptor<UserLearningActivitySummary> summaryCaptor =
        ArgumentCaptor.forClass(UserLearningActivitySummary.class);
    verify(userDailyActivityRepository).save(dailyActivityCaptor.capture());
    verify(summaryRepository).save(summaryCaptor.capture());
    assertThat(dailyActivityCaptor.getValue().getCompletedSessionCount()).isEqualTo(1);
    assertThat(dailyActivityCaptor.getValue().isActiveDay()).isTrue();
    assertThat(summaryCaptor.getValue().getUserProfileId()).isEqualTo(USER_ID);
    assertThat(persistedSummary.getCurrentStreakDays()).isEqualTo(1);
    assertThat(persistedSummary.getLongestStreakDays()).isEqualTo(1);
    assertThat(persistedSummary.getLastActivityDate()).isEqualTo(activityDate);
  }

  /** 같은 날짜의 두 번째 완료는 세션 횟수만 증가시키고 스트릭을 유지한다. */
  @Test
  void keepsStreakForRepeatedCompletionOnSameDay() {
    LocalDate activityDate = LocalDate.now(KOREA_ZONE_ID);
    UserDailyActivity dailyActivity = UserDailyActivity.startActiveDay(USER_ID, activityDate);
    when(userDailyActivityRepository.findByUserProfileIdAndActivityDate(USER_ID, activityDate))
        .thenReturn(Optional.of(dailyActivity));

    streakService.recordCompletedConversation(USER_ID, activityDate.atTime(18, 0));

    assertThat(dailyActivity.getCompletedSessionCount()).isEqualTo(2);
    verifyNoInteractions(summaryRepository);
  }

  @Test
  void activatesInactiveDayAndRecordsStreakOnNormalCompletion() {
    LocalDate activityDate = LocalDate.now(KOREA_ZONE_ID);
    UserDailyActivity dailyActivity = UserDailyActivity.startActiveDay(USER_ID, activityDate);
    ReflectionTestUtils.setField(dailyActivity, "activeDay", false);
    UserLearningActivitySummary summary = UserLearningActivitySummary.initialize(USER_ID);
    when(userDailyActivityRepository.findByUserProfileIdAndActivityDate(USER_ID, activityDate))
        .thenReturn(Optional.of(dailyActivity));
    when(summaryRepository.findById(USER_ID)).thenReturn(Optional.of(summary));

    streakService.recordCompletedConversation(USER_ID, activityDate.atTime(18, 0));

    assertThat(dailyActivity.isActiveDay()).isTrue();
    assertThat(dailyActivity.getCompletedSessionCount()).isEqualTo(2);
    assertThat(summary.getCurrentStreakDays()).isEqualTo(1);
    assertThat(summary.getLastActivityDate()).isEqualTo(activityDate);
  }

  /** 어제 활동한 사용자가 오늘 완료하면 현재 스트릭이 하루 연장된다. */
  @Test
  void extendsStreakWhenYesterdayWasActive() {
    LocalDate activityDate = LocalDate.now(KOREA_ZONE_ID);
    UserLearningActivitySummary summary = UserLearningActivitySummary.initialize(USER_ID);
    for (int daysBeforeToday = 6; daysBeforeToday >= 1; daysBeforeToday--) {
      summary.recordActiveDay(activityDate.minusDays(daysBeforeToday));
    }
    when(userDailyActivityRepository.findByUserProfileIdAndActivityDate(USER_ID, activityDate))
        .thenReturn(Optional.empty());
    when(summaryRepository.findById(USER_ID)).thenReturn(Optional.of(summary));

    streakService.recordCompletedConversation(USER_ID, activityDate.atTime(12, 0));

    assertThat(summary.getCurrentStreakDays()).isEqualTo(7);
    assertThat(summary.getLongestStreakDays()).isEqualTo(7);
  }

  /** 이틀 이상 활동하지 않은 뒤 완료하면 이전 스트릭을 이어 붙이지 않는다. */
  @Test
  void restartsStreakAfterGap() {
    LocalDate activityDate = LocalDate.now(KOREA_ZONE_ID);
    UserLearningActivitySummary summary = UserLearningActivitySummary.initialize(USER_ID);
    for (int daysBeforeToday = 5; daysBeforeToday >= 3; daysBeforeToday--) {
      summary.recordActiveDay(activityDate.minusDays(daysBeforeToday));
    }
    when(userDailyActivityRepository.findByUserProfileIdAndActivityDate(USER_ID, activityDate))
        .thenReturn(Optional.empty());
    when(summaryRepository.findById(USER_ID)).thenReturn(Optional.of(summary));

    streakService.recordCompletedConversation(USER_ID, activityDate.atTime(12, 0));

    assertThat(summary.getCurrentStreakDays()).isEqualTo(1);
    assertThat(summary.getLongestStreakDays()).isEqualTo(3);
  }

  /** 오래된 마지막 활동일은 저장값을 바꾸지 않고 현재 스트릭 0으로 조회한다. */
  @Test
  void returnsZeroForExpiredStreakWithoutUpdatingSummary() {
    UserLearningActivitySummary summary = UserLearningActivitySummary.initialize(USER_ID);
    LocalDate today = LocalDate.now(KOREA_ZONE_ID);
    summary.recordActiveDay(today.minusDays(3));
    when(summaryRepository.findById(USER_ID)).thenReturn(Optional.of(summary));

    StreakService.CurrentStreak currentStreak = streakService.getCurrentStreak(USER_ID);

    assertThat(currentStreak.currentStreakDays()).isZero();
    assertThat(currentStreak.activeToday()).isFalse();
    assertThat(currentStreak.today()).isEqualTo(today);
    assertThat(summary.getCurrentStreakDays()).isEqualTo(1);
    verify(summaryRepository, never()).save(any());
  }

  /** 달력은 요청한 월의 완료 날짜만 반환하고 전체 학습일을 함께 반환한다. */
  @Test
  void returnsOnlyRequestedMonthActiveDates() {
    LocalDate firstDate = LocalDate.of(2026, 7, 12);
    UserLearningActivitySummary summary = UserLearningActivitySummary.initialize(USER_ID);
    summary.recordActiveDay(LocalDate.now(KOREA_ZONE_ID));
    UserDailyActivity firstActivity = UserDailyActivity.startActiveDay(USER_ID, firstDate);
    UserDailyActivity secondActivity =
        UserDailyActivity.startActiveDay(USER_ID, LocalDate.of(2026, 7, 18));
    when(summaryRepository.findById(USER_ID)).thenReturn(Optional.of(summary));
    when(userDailyActivityRepository.findFirstByUserProfileIdAndActiveDayTrueOrderByActivityDateAsc(
            USER_ID))
        .thenReturn(Optional.of(firstActivity));
    when(userDailyActivityRepository.countByUserProfileIdAndActiveDayTrue(USER_ID)).thenReturn(2L);
    when(userDailyActivityRepository.findAllActiveInDateRange(
            USER_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)))
        .thenReturn(List.of(firstActivity, secondActivity));

    StreakService.StreakCalendar calendar =
        streakService.getCalendar(USER_ID, YearMonth.of(2026, 7));

    assertThat(calendar.firstActiveDate()).isEqualTo(firstDate);
    assertThat(calendar.totalActiveDays()).isEqualTo(2);
    assertThat(calendar.activeDates())
        .containsExactly(LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 18));
  }
}
