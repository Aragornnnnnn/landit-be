// 프리톡 일일 발화 사용량 예약의 한도와 날짜 분리를 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.FreeTalkDailySpeakingUsage;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.repository.FreeTalkDailySpeakingUsageRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 프리톡 일일 발화 사용량 예약의 한도와 날짜 분리를 검증한다. */
class FreeTalkDailySpeakingUsageServiceTest {

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final FreeTalkDailySpeakingUsageRepository repository =
      mock(FreeTalkDailySpeakingUsageRepository.class);
  private final UserProfileService userProfileService = mock(UserProfileService.class);
  private final FreeTalkDailySpeakingUsageService service =
      new FreeTalkDailySpeakingUsageService(repository, userProfileService);

  /** 59초 사용 뒤 3초 발화는 전체를 예약하고 남은 시간을 0으로 제한한다. */
  @Test
  void reservesEntireUtteranceThatStartsBeforeDailyLimit() {
    LocalDate usageDate = LocalDate.now(KOREA_ZONE_ID);
    FreeTalkDailySpeakingUsage usage = FreeTalkDailySpeakingUsage.create(1L, usageDate, 59_000L);
    when(repository.findByUserProfileIdAndUsageDateForUpdate(eq(1L), any(LocalDate.class)))
        .thenReturn(Optional.of(usage));

    FreeTalkDailySpeakingUsageService.DailySpeakingUsage result = service.reserve(1L, 3_000L);

    assertThat(result.usedSpeakingDurationMs()).isEqualTo(62_000L);
    assertThat(result.remainingMs()).isZero();
  }

  /** 이미 60초를 사용한 날에는 새 발화를 예약하지 않는다. */
  @Test
  void rejectsUtteranceWhenDailyLimitIsAlreadyUsed() {
    LocalDate usageDate = LocalDate.now(KOREA_ZONE_ID);
    FreeTalkDailySpeakingUsage usage = FreeTalkDailySpeakingUsage.create(1L, usageDate, 60_000L);
    when(repository.findByUserProfileIdAndUsageDateForUpdate(eq(1L), any(LocalDate.class)))
        .thenReturn(Optional.of(usage));

    assertThatThrownBy(() -> service.reserve(1L, 1L))
        .isInstanceOf(SessionException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
  }

  /** 누적 시간이 long 범위를 넘는 발화는 사용량을 음수로 되감지 않고 거절한다. */
  @Test
  void rejectsUtteranceThatOverflowsDailyUsage() {
    FreeTalkDailySpeakingUsage usage =
        FreeTalkDailySpeakingUsage.create(1L, LocalDate.now(KOREA_ZONE_ID), Long.MAX_VALUE);

    assertThatThrownBy(() -> usage.reserve(1L)).isInstanceOf(IllegalArgumentException.class);
    assertThat(usage.getUsedSpeakingDurationMs()).isEqualTo(Long.MAX_VALUE);
  }

  /** 전날 사용량과 분리해 KST 당일 사용량만 예약한다. */
  @Test
  void reservesOnlyCurrentKoreaDateUsage() {
    LocalDate usageDate = LocalDate.now(KOREA_ZONE_ID);
    FreeTalkDailySpeakingUsage previousUsage =
        FreeTalkDailySpeakingUsage.create(1L, usageDate.minusDays(1), 60_000L);
    FreeTalkDailySpeakingUsage currentUsage = FreeTalkDailySpeakingUsage.create(1L, usageDate, 0L);
    when(repository.findByUserProfileIdAndUsageDateForUpdate(eq(1L), any(LocalDate.class)))
        .thenReturn(Optional.of(currentUsage));

    service.reserve(1L, 3_000L);

    assertThat(previousUsage.getUsedSpeakingDurationMs()).isEqualTo(60_000L);
    assertThat(currentUsage.getUsedSpeakingDurationMs()).isEqualTo(3_000L);
    verify(repository).findByUserProfileIdAndUsageDateForUpdate(eq(1L), any(LocalDate.class));
  }

  /** 같은 사용자의 첫 일일 행 생성도 사용자 잠금 안에서 직렬화한다. */
  @Test
  void locksUserBeforeCreatingFirstDailyUsage() {
    when(repository.findByUserProfileIdAndUsageDateForUpdate(eq(1L), any(LocalDate.class)))
        .thenReturn(Optional.empty());
    when(repository.save(any(FreeTalkDailySpeakingUsage.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.reserve(1L, 1_000L);

    verify(userProfileService).requireActiveForUpdate(1L);
  }

  /** AI 호출에 실패하면 같은 날짜에 예약한 발화 시간을 환불한다. */
  @Test
  void releasesReservedUsage() {
    LocalDate usageDate = LocalDate.now(KOREA_ZONE_ID);
    FreeTalkDailySpeakingUsage usage = FreeTalkDailySpeakingUsage.create(1L, usageDate, 3_000L);
    when(repository.findByUserProfileIdAndUsageDateForUpdate(1L, usageDate))
        .thenReturn(Optional.of(usage));

    service.release(1L, usageDate, 1_000L);

    assertThat(usage.getUsedSpeakingDurationMs()).isEqualTo(2_000L);
  }

  /** 아직 발화 이력이 없으면 하루 전체 시간을 남은 시간으로 반환한다. */
  @Test
  void returnsEntireDailyLimitWhenUsageDoesNotExist() {
    when(repository.findByIdUserProfileIdAndIdUsageDate(eq(1L), any(LocalDate.class)))
        .thenReturn(Optional.empty());

    assertThat(service.remainingMs(1L)).isEqualTo(60_000L);
  }
}
