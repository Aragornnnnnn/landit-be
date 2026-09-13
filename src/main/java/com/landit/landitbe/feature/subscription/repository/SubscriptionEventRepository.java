// 사용자별 구독 결제 이력을 저장하고 조회한다.

package com.landit.landitbe.feature.subscription.repository;

import com.landit.landitbe.feature.subscription.domain.SubscriptionEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자별 구독 결제 이력을 저장하고 조회한다. */
public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {

  /**
   * 같은 RevenueCat 이벤트 ID가 이미 저장됐는지 확인한다.
   *
   * @param eventId RevenueCat 이벤트 ID
   * @return 이미 저장됐으면 {@code true}
   */
  boolean existsByEventId(String eventId);

  /**
   * 사용자의 결제 이력을 발생 시각 내림차순으로 최근 50개 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 최근 50개 결제 이력. 발생 시각이 같으면 나중에 저장한 것이 앞선다
   */
  List<SubscriptionEvent> findTop50ByUserProfileIdOrderByOccurredAtDescIdDesc(Long userProfileId);
}
