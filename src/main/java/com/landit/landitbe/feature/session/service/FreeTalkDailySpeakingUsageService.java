// 프리톡의 일일 발화 사용량 예약을 처리한다.

package com.landit.landitbe.feature.session.service;

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

  /** KST 당일 사용량을 잠금 처리하며 새 발화를 한 번 예약한다. */
  @Transactional
  public DailySpeakingUsage reserve(long userId, long utteranceDurationMs) {
    LocalDate usageDate = LocalDate.now(KOREA_ZONE_ID);
    FreeTalkDailySpeakingUsage usage =
        repository
            .findByUserProfileIdAndUsageDateForUpdate(userId, usageDate)
            .orElseGet(
                () -> repository.save(FreeTalkDailySpeakingUsage.create(userId, usageDate, 0L)));
    if (remainingMs(usage.getUsedSpeakingDurationMs()) == 0) {
      throw new SessionException(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
    }
    usage.reserve(utteranceDurationMs);
    return new DailySpeakingUsage(usage.getUsedSpeakingDurationMs(), remainingMs(usage));
  }

  private long remainingMs(FreeTalkDailySpeakingUsage usage) {
    return remainingMs(usage.getUsedSpeakingDurationMs());
  }

  private long remainingMs(long usedSpeakingDurationMs) {
    return Math.max(0L, DAILY_SPEAKING_LIMIT_MS - usedSpeakingDurationMs);
  }

  /** 예약 후 누적 발화 시간과 남은 시간을 반환한다. */
  public record DailySpeakingUsage(long usedSpeakingDurationMs, long remainingMs) {}
}
