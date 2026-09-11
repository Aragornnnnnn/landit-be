// 유료 구독 도입 시각의 경계와 요청별 접근 판정, 실제 구독 상태 보존을 검증한다.

package com.landit.landitbe.feature.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.dto.UserSubscriptionSnapshot;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.repository.SubscriptionEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** 유료 구독 도입 전후의 접근 권한과 조회 응답을 검증한다. */
class UserSubscriptionServiceTest {

  private static final long USER_ID = 1L;
  private static final Instant LAUNCH_INSTANT = Instant.parse("2026-09-13T05:44:00Z");
  private static final String LAUNCHED_AT = "2026-09-13T14:44:00+09:00";
  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime LAUNCH_TIME =
      LocalDateTime.ofInstant(LAUNCH_INSTANT, SERVICE_ZONE);

  private final UserProfileService userProfileService = mock(UserProfileService.class);
  private final LearningProgressService learningProgressService =
      mock(LearningProgressService.class);

  /** 미설정 또는 미래 도입 시각이면 구독과 완료 이력을 조회하지 않고 모든 유료 기능 게이트를 연다. */
  @ParameterizedTest
  @ValueSource(strings = {"", LAUNCHED_AT})
  void allowsEverythingBeforeLaunch(String launchedAt) {
    var service = service(launchedAt, Clock.fixed(LAUNCH_INSTANT.minusNanos(1), SERVICE_ZONE));
    when(userProfileService.getSubscription(USER_ID)).thenReturn(snapshot(false));

    var access = service.evaluateAccess(USER_ID);

    assertThat(access.launched()).isFalse();
    assertThat(access.allowsPremiumOnlyFeature()).isTrue();
    assertThat(access.allowsScenarioConversation()).isTrue();
    verifyNoInteractions(userProfileService, learningProgressService);
  }

  /** 도입 시각과 같거나 이후이면 시간대 표기와 관계없이 기존 비구독자 제한을 적용한다. */
  @ParameterizedTest
  @CsvSource({
    "2026-09-13T14:44:00+09:00, 0",
    "2026-09-13T05:44:00Z, 0",
    "2026-09-13T14:44:00+09:00, 1"
  })
  void appliesExistingPolicyAtAndAfterLaunch(String launchedAt, long elapsedNanos) {
    var service =
        service(launchedAt, Clock.fixed(LAUNCH_INSTANT.plusNanos(elapsedNanos), SERVICE_ZONE));
    when(userProfileService.getSubscription(USER_ID)).thenReturn(snapshot(false));

    var access = service.evaluateAccess(USER_ID);

    assertThat(access.launched()).isTrue();
    assertThat(access.allowsPremiumOnlyFeature()).isFalse();
    assertThat(access.allowsScenarioConversation()).isTrue();
    when(learningProgressService.hasClearedScenarioSince(USER_ID, LAUNCH_TIME)).thenReturn(true);
    assertThat(service.evaluateAccess(USER_ID).allowsScenarioConversation()).isFalse();
    assertThat(service.getSubscription(USER_ID).conversationCompletedSinceLaunch()).isTrue();

    when(userProfileService.getSubscription(USER_ID)).thenReturn(snapshot(true));
    assertThat(service.evaluateAccess(USER_ID).allowsPremiumOnlyFeature()).isTrue();
    assertThat(service.evaluateAccess(USER_ID).allowsScenarioConversation()).isTrue();
  }

  /** 같은 서비스 인스턴스도 다음 요청의 시각이 도입 시각에 도달하면 제한을 시작한다. */
  @Test
  void activatesWithoutRestartWhenClockReachesLaunch() {
    Clock clock = mock(Clock.class);
    when(clock.getZone()).thenReturn(SERVICE_ZONE);
    when(clock.instant()).thenReturn(LAUNCH_INSTANT.minusNanos(1));
    when(userProfileService.getSubscription(USER_ID)).thenReturn(snapshot(false));
    var service = service(LAUNCHED_AT, clock);
    assertThat(service.evaluateAccess(USER_ID).allowsPremiumOnlyFeature()).isTrue();

    when(clock.instant()).thenReturn(LAUNCH_INSTANT);
    assertThat(service.evaluateAccess(USER_ID).allowsPremiumOnlyFeature()).isFalse();
  }

  /** 도입 전 구독 조회는 실제 구독 여부를 보존하고 완료 이력을 조회하지 않는다. */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preservesSubscriptionAndSkipsCompletionBeforeLaunch(boolean premium) {
    var service = service(LAUNCHED_AT, Clock.fixed(LAUNCH_INSTANT.minusNanos(1), SERVICE_ZONE));
    when(userProfileService.getSubscription(USER_ID)).thenReturn(snapshot(premium));

    var response = service.getSubscription(USER_ID);

    assertThat(response.premium()).isEqualTo(premium);
    assertThat(response.subscriptionStatus()).isEqualTo(snapshot(premium).subscriptionStatus());
    assertThat(response.conversationCompletedSinceLaunch()).isFalse();
    verifyNoInteractions(learningProgressService);
  }

  private UserSubscriptionService service(String launchedAt, Clock clock) {
    return new UserSubscriptionService(
        userProfileService,
        learningProgressService,
        mock(SubscriptionEventRepository.class),
        new SubscriptionProperties(launchedAt),
        clock);
  }

  private UserSubscriptionSnapshot snapshot(boolean premium) {
    return new UserSubscriptionSnapshot(
        premium ? SubscriptionStatus.ACTIVE : SubscriptionStatus.NONE,
        premium,
        null,
        null,
        null,
        null);
  }
}
