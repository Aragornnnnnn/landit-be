// 프리톡의 일일 발화 사용량 예약을 처리한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.config.session.FreeTalkProperties;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.domain.FreeTalkDailySpeakingUsage;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.repository.FreeTalkDailySpeakingUsageRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡의 일일 발화 사용량 예약을 처리한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkDailySpeakingUsageService {

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final FreeTalkDailySpeakingUsageRepository repository;
  private final UserProfileService userProfileService;
  private final FreeTalkProperties properties;
  private final Clock clock;

  /**
   * 현재 환경의 일일 사용자 발화 제한시간을 반환한다.
   *
   * @return 일일 사용자 발화 제한시간 밀리초
   */
  public long speakingTimeLimitMs() {
    return properties.speakingTimeLimitMs();
  }

  /**
   * KST 당일의 남은 발화 시간을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 당일 남은 사용자 발화 시간 밀리초
   */
  @Transactional(readOnly = true)
  public long remainingMs(long userId) {
    return usage(userId).remainingMs();
  }

  /**
   * KST 당일의 사용 시간과 남은 시간을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 당일 사용 시간과 남은 시간을 담은 요약
   */
  @Transactional(readOnly = true)
  public DailySpeakingUsage usage(long userId) {
    LocalDate usageDate = LocalDate.now(clock.withZone(KOREA_ZONE_ID));
    return repository
        .findByIdUserProfileIdAndIdUsageDate(userId, usageDate)
        .map(
            usage ->
                new DailySpeakingUsage(
                    usageDate, usage.getUsedSpeakingDurationMs(), remainingForUsage(usage)))
        .orElse(new DailySpeakingUsage(usageDate, 0L, speakingTimeLimitMs()));
  }

  /**
   * KST 당일에 새 프리톡 세션을 시작할 수 있는지 확인한다.
   *
   * @param userId 사용자 ID
   * @throws SessionException 당일 발화 한도를 모두 사용했을 때
   */
  @Transactional(readOnly = true)
  public void requireRemaining(long userId) {
    if (remainingMs(userId) == 0) {
      throw new SessionException(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
    }
  }

  /**
   * KST 당일 사용량을 잠금 처리하며 새 발화를 한 번 예약한다.
   *
   * @param userId 사용자 ID
   * @param utteranceDurationMs 예약할 사용자 발화 시간 밀리초
   * @return 예약 후 당일 사용 시간과 남은 시간 요약
   * @throws SessionException 당일 발화 한도 또는 일일·분당 요청 한도에 도달했을 때
   * @throws IllegalArgumentException 발화 시간이 음수이거나 누적값이 long 범위를 넘을 때
   */
  @Transactional
  public DailySpeakingUsage reserve(long userId, long utteranceDurationMs) {
    userProfileService.requireActiveForUpdate(userId);
    LocalDateTime now = LocalDateTime.now(clock.withZone(KOREA_ZONE_ID));
    LocalDate usageDate = now.toLocalDate();
    FreeTalkDailySpeakingUsage usage = findOrCreateUsage(userId, usageDate);
    if (remainingForUsedDurationMs(usage.getUsedSpeakingDurationMs()) == 0) {
      throw new SessionException(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
    }
    recordRequestWithinLimits(usage, now);
    usage.reserve(utteranceDurationMs);
    return new DailySpeakingUsage(
        usageDate, usage.getUsedSpeakingDurationMs(), remainingForUsage(usage));
  }

  /**
   * 발화 시간이 없는 세션 시작·종료 결정·표현 재시도의 생성 요청을 기록한다.
   *
   * <p>호출자의 예약 트랜잭션에서 기록하고 외부 AI 호출 전에 커밋한다. AI 실패 후에도 요청 횟수는 유지한다.
   *
   * @param userId 요청 사용자 ID
   * @throws SessionException 일일 또는 분당 요청 한도에 도달했을 때
   */
  @Transactional
  public void reserveRequest(long userId) {
    userProfileService.requireActiveForUpdate(userId);
    LocalDateTime now = LocalDateTime.now(clock.withZone(KOREA_ZONE_ID));
    recordRequestWithinLimits(findOrCreateUsage(userId, now.toLocalDate()), now);
  }

  private FreeTalkDailySpeakingUsage findOrCreateUsage(long userId, LocalDate usageDate) {
    return repository
        .findByUserProfileIdAndUsageDateForUpdate(userId, usageDate)
        .orElseGet(() -> repository.save(FreeTalkDailySpeakingUsage.create(userId, usageDate, 0L)));
  }

  private void recordRequestWithinLimits(FreeTalkDailySpeakingUsage usage, LocalDateTime now) {
    if (usage.getRequestCount() >= properties.dailyRequestLimit()) {
      throw new SessionException(SessionErrorCode.FREE_TALK_DAILY_REQUEST_LIMIT_EXCEEDED);
    }
    LocalDateTime minute = now.truncatedTo(ChronoUnit.MINUTES);
    if (minute.equals(usage.getRequestMinute())
        && usage.getMinuteRequestCount() >= properties.requestsPerMinuteLimit()) {
      throw new SessionException(SessionErrorCode.FREE_TALK_REQUEST_RATE_LIMIT_EXCEEDED);
    }
    usage.recordRequest(minute);
  }

  /**
   * 실패한 AI 요청에서 예약한 일일 발화 시간을 되돌린다.
   *
   * @param userId 사용자 ID
   * @param usageDate 예약이 기록된 KST 날짜
   * @param utteranceDurationMs 되돌릴 사용자 발화 시간 밀리초
   * @throws IllegalStateException 예약한 일일 사용량을 찾을 수 없을 때
   */
  @Transactional
  public void release(long userId, LocalDate usageDate, long utteranceDurationMs) {
    userProfileService.requireActiveForUpdate(userId);
    FreeTalkDailySpeakingUsage usage =
        repository
            .findByUserProfileIdAndUsageDateForUpdate(userId, usageDate)
            .orElseThrow(() -> new IllegalStateException("예약한 일일 발화 사용량이 없습니다."));
    usage.release(utteranceDurationMs);
  }

  private long remainingForUsage(FreeTalkDailySpeakingUsage usage) {
    return remainingForUsedDurationMs(usage.getUsedSpeakingDurationMs());
  }

  private long remainingForUsedDurationMs(long usedSpeakingDurationMs) {
    return Math.max(0L, speakingTimeLimitMs() - usedSpeakingDurationMs);
  }

  /**
   * 예약 후 일일 누적 발화 시간과 남은 시간을 반환한다.
   *
   * @param usageDate 사용량을 집계한 KST 날짜
   * @param usedSpeakingDurationMs KST 당일 사용한 사용자 발화 시간 밀리초
   * @param remainingMs KST 당일 남은 사용자 발화 시간 밀리초
   */
  public record DailySpeakingUsage(
      LocalDate usageDate, long usedSpeakingDurationMs, long remainingMs) {}
}
