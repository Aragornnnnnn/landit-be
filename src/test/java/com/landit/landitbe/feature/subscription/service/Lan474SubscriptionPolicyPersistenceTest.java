// H2에서 공개 정책 버전과 심사 대상 계정이 실제로 저장되고 다시 조회되는지 검증한다.

package com.landit.landitbe.feature.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.subscription.domain.SubscriptionLaunchPolicy.Mode;
import com.landit.landitbe.feature.subscription.repository.SubscriptionLaunchPolicyRepository;
import com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService.Change;
import com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService.Policy;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** Flyway 스키마와 JPA 버전 필드를 함께 사용해 실행 정책의 갱신 계약을 검증한다. */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class Lan474SubscriptionPolicyPersistenceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-11T03:00:00Z"), ZoneId.of("Asia/Seoul"));
  private static final OffsetDateTime LAUNCHED_AT = OffsetDateTime.parse("2026-09-11T02:00:00Z");
  @Autowired private SubscriptionLaunchPolicyRepository repository;
  @Autowired private TestEntityManager entityManager;
  private SubscriptionLaunchPolicyService service;

  /** 실행 환경의 기존 오픈 설정과 DB에 저장하는 새 정책을 구분한다. */
  @BeforeEach
  void setUp() {
    service =
        new SubscriptionLaunchPolicyService(
            repository, new SubscriptionProperties(LAUNCHED_AT.toString()), CLOCK);
  }

  /** OFF를 저장하면 이전 환경변수보다 우선하며 갱신 버전은 DB 재조회에도 유지된다. */
  @Test
  void savedOffPolicyOverridesLegacyConfigurationAndAdvancesVersion() {
    assertThat(service.current().mode()).isEqualTo(Mode.ALL);
    Policy off = service.update(new Change(0, Mode.OFF, null, false, Set.of()));
    entityManager.clear();

    assertThat(off.version()).isEqualTo(1);
    assertThat(service.current()).isEqualTo(off);
    assertThat(service.enabledFor(service.current(), 10L)).isFalse();

    Policy enabled =
        service.update(new Change(off.version(), Mode.ALL, LAUNCHED_AT, false, Set.of()));
    entityManager.clear();
    assertThat(enabled.version()).isEqualTo(2);
    assertThat(service.current()).isEqualTo(enabled);
  }

  /** 오래된 관리 화면이 최신 정책을 덮어쓰지 못한다. */
  @Test
  void staleAdminVersionCannotOverwriteTheCurrentPolicy() {
    Policy initial = service.update(new Change(0, Mode.ALL, LAUNCHED_AT, false, Set.of()));
    Policy paused =
        service.update(new Change(initial.version(), Mode.ALL, LAUNCHED_AT, true, Set.of()));
    entityManager.clear();

    assertThatThrownBy(
            () -> service.update(new Change(initial.version(), Mode.OFF, null, false, Set.of())))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    assertThat(service.current()).isEqualTo(paused);
  }

  /** 심사 계정 집합을 DB에서 재조회해도 일반 사용자는 심사 제한에서 제외한다. */
  @Test
  void persistedReviewAudienceIsAppliedOnlyToItsMembers() {
    UserProfile reviewer =
        entityManager.persistAndFlush(new UserProfile("lan474-review@example.com", "심사 계정", null));
    final UserProfile ordinary =
        entityManager.persistAndFlush(new UserProfile("lan474-user@example.com", "일반 계정", null));
    service.update(new Change(0, Mode.REVIEW, LAUNCHED_AT, false, Set.of(reviewer.getId())));
    entityManager.clear();

    Policy loaded = service.current();
    assertThat(loaded.reviewUserIds()).containsExactly(reviewer.getId());
    assertThat(service.enabledFor(loaded, reviewer.getId())).isTrue();
    assertThat(service.enabledFor(loaded, ordinary.getId())).isFalse();
  }
}
