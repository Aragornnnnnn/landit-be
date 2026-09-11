// 기존 SSM 오픈 시각으로 구독 제한의 적용 여부를 판단한다.

package com.landit.landitbe.feature.subscription.service;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 결제 공개 제어를 기존 환경변수 설정에 유지한다. */
@Service
@RequiredArgsConstructor
public class SubscriptionLaunchPolicyService {
  private final SubscriptionProperties properties;
  private final Clock clock;

  /**
   * 서버 시작 시 SSM에서 주입한 오픈 시각을 사용한다. DB에는 공개 스위치를 저장하지 않는다.
   *
   * @return 기존 설정의 공개 정책. 버전과 새 시작 중지는 이번 범위에서 사용하지 않는다.
   */
  public Policy current() {
    LocalDateTime at =
        properties
            .launchedAtOrEmpty()
            .map(value -> value.atZoneSameInstant(clock.getZone()).toLocalDateTime())
            .orElse(null);
    return new Policy(0, at, false);
  }

  /**
   * 기존 SSM 정책은 모든 사용자에게 같은 오픈 시각을 적용한다.
   *
   * @param policy 같은 요청에서 조회한 공개 정책
   * @param userId 학습 사용자 ID
   * @return 실제 활성 시각에 도달했으면 true
   */
  public boolean enabledFor(Policy policy, long userId) {
    return active(policy);
  }

  /**
   * 오픈 시각이 없거나 아직 미래라면 유료 제한을 적용하지 않는다.
   *
   * @param policy 같은 요청에서 조회한 공개 정책
   * @return 실제 활성 시각에 도달했으면 true
   */
  public boolean active(Policy policy) {
    return policy.effectiveAt() != null && !LocalDateTime.now(clock).isBefore(policy.effectiveAt());
  }

  /** 환경변수로 읽은 공개 정책이며 별도 DB 스위치와 신규 시작 중지를 제공하지 않는다. */
  public record Policy(long version, LocalDateTime effectiveAt, boolean newStartsPaused) {}
}
