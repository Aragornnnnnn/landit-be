// 사용자별 구독 결제 이력을 저장하고 조회한다.

package com.landit.landitbe.feature.subscription.event.repository;

import com.landit.landitbe.feature.subscription.event.domain.SubscriptionEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * 결제 금액이 양수인 구매·갱신·상품 변경 중 최신 이력을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 무료 체험·무료 프로모션을 제외한 최신 결제. 없으면 빈 값
   */
  @Query(
      value =
          """
          SELECT * FROM subscription_event
          WHERE user_profile_id = :userId
            AND type IN ('INITIAL_PURCHASE', 'RENEWAL', 'PRODUCT_CHANGE')
            AND price > 0
            AND (period_type IS NULL OR period_type NOT IN ('TRIAL', 'PROMOTIONAL'))
          ORDER BY occurred_at DESC, id DESC
          LIMIT 1
          """,
      nativeQuery = true)
  Optional<SubscriptionEvent> findLatestPayment(@Param("userId") Long userId);
}
