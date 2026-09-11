// 공개 정책의 실제 시각과 심사 대상 범위가 사용자별 접근에 반영되는지 검증한다.

package com.landit.landitbe.feature.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.subscription.domain.SubscriptionLaunchPolicy.Mode;
import com.landit.landitbe.feature.subscription.repository.SubscriptionLaunchPolicyRepository;
import com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService.Change;
import com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService.Policy;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 구독 공개가 환경변수의 존재만으로 조기 활성화되지 않도록 검증한다. */
class Lan474SubscriptionLaunchPolicyServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
  private final SubscriptionLaunchPolicyRepository repository =
      mock(SubscriptionLaunchPolicyRepository.class);
  private final SubscriptionLaunchPolicyService service =
      new SubscriptionLaunchPolicyService(repository, new SubscriptionProperties(""), CLOCK);

  /** OFF이면 과거 시각과 심사 계정이 남아 있어도 어느 사용자에게도 제한을 적용하지 않는다. */
  @Test
  void offRemainsDisabledEvenWithPastTimeAndReviewUsers() {
    Policy policy = new Policy(3, Mode.OFF, NOW.minusDays(1), false, Set.of(10L));

    assertThat(service.active(policy)).isFalse();
    assertThat(service.enabledFor(policy, 10L)).isFalse();
    assertThat(service.enabledFor(policy, 20L)).isFalse();
  }

  /** 예약된 시각 전에는 ALL도 비활성이며 정확히 오픈 시각부터 활성화한다. */
  @Test
  void futureLaunchWaitsForTheActualClockBoundary() {
    Policy future = new Policy(1, Mode.ALL, NOW.plusSeconds(1), false, Set.of());
    Policy due = new Policy(2, Mode.ALL, NOW, false, Set.of());

    assertThat(service.enabledFor(future, 20L)).isFalse();
    assertThat(service.enabledFor(due, 20L)).isTrue();
  }

  /** 심사 기간의 제한은 지정한 심사 계정에만 적용한다. */
  @Test
  void reviewModeRestrictsOnlySelectedAccounts() {
    Policy review = new Policy(2, Mode.REVIEW, NOW.minusSeconds(1), false, Set.of(10L));

    assertThat(service.enabledFor(review, 10L)).isTrue();
    assertThat(service.enabledFor(review, 20L)).isFalse();
  }

  /** 전체 공개로 전환하면 심사 목록 포함 여부와 무관하게 제한한다. */
  @Test
  void allModeIncludesOrdinaryAccounts() {
    Policy all = new Policy(3, Mode.ALL, NOW.minusSeconds(1), false, Set.of(10L));

    assertThat(service.enabledFor(all, 10L)).isTrue();
    assertThat(service.enabledFor(all, 20L)).isTrue();
  }

  /** DB 정책이 없으면 시간대가 명시된 기존 설정을 서비스 시간대로 변환해 사용한다. */
  @Test
  void legacyConfigurationUsesTheSameInstantWithoutPrematureLaunch() {
    SubscriptionLaunchPolicyService legacy =
        new SubscriptionLaunchPolicyService(
            repository, new SubscriptionProperties("2026-09-11T03:00:01Z"), CLOCK);

    Policy policy = legacy.current();

    assertThat(policy.version()).isZero();
    assertThat(policy.effectiveAt()).isEqualTo(NOW.plusSeconds(1));
    assertThat(legacy.enabledFor(policy, 20L)).isFalse();
    assertThat(service.current().mode()).isEqualTo(Mode.OFF);
  }

  /** 시작 시각 없는 공개 요청은 DB 변경 전에 거절한다. */
  @Test
  void activeModeRequiresAnExplicitLaunchInstant() {
    assertThatThrownBy(() -> service.update(new Change(0, Mode.ALL, null, false, Set.of())))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    verifyNoInteractions(repository);
  }
}
