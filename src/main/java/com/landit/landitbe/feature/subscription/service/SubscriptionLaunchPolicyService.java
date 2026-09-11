// 실제 시각과 심사 대상 계정으로 구독 정책의 적용 여부를 판단한다.

package com.landit.landitbe.feature.subscription.service;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.subscription.domain.SubscriptionLaunchPolicy;
import com.landit.landitbe.feature.subscription.domain.SubscriptionLaunchPolicy.Mode;
import com.landit.landitbe.feature.subscription.repository.SubscriptionLaunchPolicyRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FE와 BE가 공유할 실행 정책을 DB에서 읽는다. */
@Service
@RequiredArgsConstructor
public class SubscriptionLaunchPolicyService {
  private final SubscriptionLaunchPolicyRepository repository;
  private final SubscriptionProperties properties;
  private final Clock clock;

  /**
   * DB 설정 전에는 기존 환경변수를 호환용 초기값으로 사용한다.
   *
   * @return DB 정책 또는 최초 환경변수 정책
   */
  @Transactional(readOnly = true)
  public Policy current() {
    return repository
        .findById(1L)
        .map(this::from)
        .orElseGet(
            () -> {
              LocalDateTime at =
                  properties
                      .launchedAtOrEmpty()
                      .map(value -> value.atZoneSameInstant(clock.getZone()).toLocalDateTime())
                      .orElse(null);
              return new Policy(0, at == null ? Mode.OFF : Mode.ALL, at, false, Set.of());
            });
  }

  /**
   * 서버 시각 기준으로 해당 사용자에게 정책이 활성화됐는지 판별한다.
   *
   * @param policy 같은 요청에서 조회한 공개 정책
   * @param userId 학습 사용자 ID
   * @return 현재 시각에 해당 계정으로 공개됐으면 true
   */
  public boolean enabledFor(Policy policy, long userId) {
    return active(policy) && (policy.mode() == Mode.ALL || policy.reviewUserIds().contains(userId));
  }

  /**
   * 평가 활성화 등 공개 시점이 필요한 기능에 실제 활성 여부를 제공한다.
   *
   * @param policy 같은 요청에서 조회한 공개 정책
   * @return 실제 활성 시각에 도달했으면 true
   */
  public boolean active(Policy policy) {
    return policy.mode() != Mode.OFF
        && policy.effectiveAt() != null
        && !LocalDateTime.now(clock).isBefore(policy.effectiveAt());
  }

  /**
   * 관리자 전용 API에서 예상 버전을 비교한 뒤 정책을 변경한다.
   *
   * @param change 예상 버전과 새 공개 정책
   * @return 저장한 정책과 갱신 버전
   */
  @Transactional
  public Policy update(Change change) {
    if (change.mode() == null
        || change.reviewUserIds() == null
        || change.reviewUserIds().size() > 100
        || change.reviewUserIds().stream().anyMatch(id -> id == null || id <= 0)
        || (change.mode() != Mode.OFF && change.effectiveAt() == null)) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    SubscriptionLaunchPolicy entity = repository.findById(1L).orElse(null);
    long version = entity == null ? 0 : entity.getVersion() + 1;
    if (change.expectedVersion() != version) {
      throw new ApiException(ErrorCode.CONFLICT);
    }
    LocalDateTime at =
        change.effectiveAt() == null
            ? null
            : change.effectiveAt().atZoneSameInstant(clock.getZone()).toLocalDateTime();
    if (entity == null) {
      entity =
          new SubscriptionLaunchPolicy(
              change.mode(), at, change.newStartsPaused(), change.reviewUserIds());
    } else {
      entity.update(change.mode(), at, change.newStartsPaused(), change.reviewUserIds());
    }
    return from(repository.saveAndFlush(entity));
  }

  private Policy from(SubscriptionLaunchPolicy entity) {
    return new Policy(
        entity.getVersion() + 1,
        entity.getMode(),
        entity.getEffectiveAt(),
        entity.isNewStartsPaused(),
        Set.copyOf(entity.getReviewUserIds()));
  }

  /** 실행 중인 공개 정책이며 심사 계정 목록은 관리자 응답에서만 노출한다. */
  public record Policy(
      long version,
      Mode mode,
      LocalDateTime effectiveAt,
      boolean newStartsPaused,
      Set<Long> reviewUserIds) {}

  /** 시간대가 명시된 시각과 현재 버전을 받는 관리자 변경 요청이다. */
  public record Change(
      long expectedVersion,
      Mode mode,
      OffsetDateTime effectiveAt,
      boolean newStartsPaused,
      Set<Long> reviewUserIds) {}
}
