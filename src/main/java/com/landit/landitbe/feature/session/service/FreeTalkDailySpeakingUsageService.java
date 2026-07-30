// 프리톡의 일일 발화 사용량 예약을 처리한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.FreeTalkDailySpeakingUsage;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.repository.FreeTalkDailySpeakingUsageRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡의 일일 발화 사용량 예약을 처리한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkDailySpeakingUsageService {

  private static final long DAILY_SPEAKING_LIMIT_MS = 60_000L;
  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final FreeTalkDailySpeakingUsageRepository repository;
  private final UserProfileService userProfileService;

  /** KST 당일의 남은 발화 시간을 조회한다. */
  @Transactional(readOnly = true)
  public long remainingMs(long userId) {
    return usage(userId).remainingMs();
  }

  /** KST 당일의 사용 시간과 남은 시간을 조회한다. */
  @Transactional(readOnly = true)
  public DailySpeakingUsage usage(long userId) {
    LocalDate usageDate = LocalDate.now(KOREA_ZONE_ID);
    return repository
        .findByIdUserProfileIdAndIdUsageDate(userId, usageDate)
        .map(
            usage ->
                new DailySpeakingUsage(usage.getUsedSpeakingDurationMs(), remainingForUsage(usage)))
        .orElse(new DailySpeakingUsage(0L, DAILY_SPEAKING_LIMIT_MS));
  }

  /** KST 당일에 새 프리톡 세션을 시작할 수 있는지 확인한다. */
  @Transactional(readOnly = true)
  public void requireRemaining(long userId) {
    if (remainingMs(userId) == 0) {
      throw new SessionException(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
    }
  }

  /** KST 당일 사용량을 잠금 처리하며 새 발화를 한 번 예약한다. */
  @Transactional
  public DailySpeakingUsage reserve(long userId, long utteranceDurationMs) {
    LocalDate usageDate = LocalDate.now(KOREA_ZONE_ID);
    userProfileService.requireActiveForUpdate(userId);
    FreeTalkDailySpeakingUsage usage =
        repository
            .findByUserProfileIdAndUsageDateForUpdate(userId, usageDate)
            .orElseGet(
                () -> repository.save(FreeTalkDailySpeakingUsage.create(userId, usageDate, 0L)));
    if (remainingForUsedDurationMs(usage.getUsedSpeakingDurationMs()) == 0) {
      throw new SessionException(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
    }
    usage.reserve(utteranceDurationMs);
    return new DailySpeakingUsage(usage.getUsedSpeakingDurationMs(), remainingForUsage(usage));
  }

  private long remainingForUsage(FreeTalkDailySpeakingUsage usage) {
    return remainingForUsedDurationMs(usage.getUsedSpeakingDurationMs());
  }

  private long remainingForUsedDurationMs(long usedSpeakingDurationMs) {
    return Math.max(0L, DAILY_SPEAKING_LIMIT_MS - usedSpeakingDurationMs);
  }

  /**
   * 예약 후 일일 누적 발화 시간과 남은 시간을 반환한다.
   *
   * @param usedSpeakingDurationMs KST 당일 사용한 사용자 발화 시간 밀리초
   * @param remainingMs KST 당일 남은 사용자 발화 시간 밀리초
   */
  public record DailySpeakingUsage(long usedSpeakingDurationMs, long remainingMs) {}
}
