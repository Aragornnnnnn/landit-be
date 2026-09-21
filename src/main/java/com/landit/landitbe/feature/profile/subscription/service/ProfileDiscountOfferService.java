// 계정별 최초 페이월 이탈 할인 기회를 부여하고 남은 시간을 조회한다.

package com.landit.landitbe.feature.profile.subscription.service;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.exception.UserProfileErrorCode;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.feature.profile.subscription.dto.DiscountOffer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계정별 할인 기회를 구독 변경과 같은 프로필 잠금으로 보호한다. */
@Service
@RequiredArgsConstructor
public class ProfileDiscountOfferService {
  private final UserProfileRepository profiles;
  private final Clock clock;

  /**
   * 비구독자의 첫 이탈에서만 5분 할인을 부여하며 기존 기록은 만료 후에도 유지한다.
   *
   * @param userId 인증된 사용자 ID
   * @return 유효한 할인 기회. 프리미엄이거나 만료됐으면 null
   * @throws UserProfileException 활성 사용자가 없을 때
   */
  @Transactional
  public DiscountOffer dismiss(Long userId) {
    UserProfile profile =
        profiles
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
    LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    if (isPremium(profile, now)) {
      return null;
    }
    if (profile.getDiscountOfferExpiresAt() == null) {
      boolean newUser =
          !now.isBefore(profile.getCreatedAt()) && now.isBefore(profile.getCreatedAt().plusDays(7));
      profile.grantDiscountOffer(now.plusMinutes(5), newUser);
    }
    return activeOffer(profile, now);
  }

  /**
   * 상태를 변경하지 않고 현재 할인 기회를 조회한다.
   *
   * @param userId 인증된 사용자 ID
   * @return 미부여·만료·프리미엄 상태이면 null
   * @throws UserProfileException 활성 사용자가 없을 때
   */
  @Transactional(readOnly = true)
  public DiscountOffer findActive(Long userId) {
    UserProfile profile =
        profiles
            .findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
            .orElseThrow(() -> new UserProfileException(UserProfileErrorCode.INVALID_TOKEN));
    return activeOffer(profile, LocalDateTime.now(clock));
  }

  private DiscountOffer activeOffer(UserProfile profile, LocalDateTime now) {
    LocalDateTime expiresAt = profile.getDiscountOfferExpiresAt();
    if (isPremium(profile, now) || expiresAt == null || !now.isBefore(expiresAt)) {
      return null;
    }
    Duration remaining = Duration.between(now, expiresAt);
    long seconds = remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
    return new DiscountOffer(
        seconds, expiresAt, Boolean.TRUE.equals(profile.getDiscountOfferNewUser()));
  }

  private boolean isPremium(UserProfile profile, LocalDateTime now) {
    return profile.isPremium()
        && (profile.getSubscriptionExpiresAt() == null
            || now.isBefore(profile.getSubscriptionExpiresAt()));
  }
}
