// 할인 만료 경계와 부여 당시 신규 혜택 판정의 고정을 검증한다.

package com.landit.landitbe.feature.profile.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/** 할인 만료와 신규 혜택의 시간 경계를 고정된 서버 시각으로 검증한다. */
class ProfileDiscountOfferServiceTest {
  private static final Instant NOW = Instant.parse("2026-09-21T05:00:00Z");
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime LOCAL_NOW = LocalDateTime.ofInstant(NOW, ZONE);
  private final UserProfileRepository profiles = mock(UserProfileRepository.class);
  private final UserProfile profile = new UserProfile("promo@example.com", "할인", 1L);

  @ParameterizedTest
  @CsvSource({"-1, true", "0, false", "1, false"})
  @DisplayName("가입 후 7일에 도달하기 전까지만 신규 혜택으로 저장한다.")
  void freezesNewUserLabelAtSevenDayBoundary(long offsetSeconds, boolean expected) {
    ReflectionTestUtils.setField(
        profile, "createdAt", LOCAL_NOW.minusDays(7).minusSeconds(offsetSeconds));
    when(profiles.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(profile));
    var offer = service(NOW).dismiss(1L);
    assertThat(offer.newUser()).isEqualTo(expected);
    assertThat(offer.remainingSeconds()).isEqualTo(300);
    assertThat(offer.expiresAt()).isEqualTo(LOCAL_NOW.plusMinutes(5));
    assertThat(service(NOW.plusSeconds(60)).dismiss(1L).newUser()).isEqualTo(expected);
  }

  @Test
  @DisplayName("만료 직전 소수 초는 1초로 표시하고 만료 시각부터 null을 반환하며 기록은 유지한다.")
  void expiresExactlyAtDeadline() {
    profile.grantDiscountOffer(LOCAL_NOW.plusMinutes(5), true);
    when(profiles.findByIdAndStatus(1L, UserProfileStatus.ACTIVE)).thenReturn(Optional.of(profile));
    when(profiles.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(profile));
    assertThat(service(NOW.plusSeconds(300).minusNanos(1)).findActive(1L).remainingSeconds())
        .isEqualTo(1);
    assertThat(service(NOW.plusSeconds(300)).findActive(1L)).isNull();
    assertThat(service(NOW.plusSeconds(301)).dismiss(1L)).isNull();
    assertThat(profile.getDiscountOfferExpiresAt()).isEqualTo(LOCAL_NOW.plusMinutes(5));
    assertThat(profile.getDiscountOfferNewUser()).isTrue();
  }

  private ProfileDiscountOfferService service(Instant instant) {
    return new ProfileDiscountOfferService(profiles, Clock.fixed(instant, ZONE));
  }
}
