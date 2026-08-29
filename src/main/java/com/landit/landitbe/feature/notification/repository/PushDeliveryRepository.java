// 중복 방지 키와 식별자로 푸시 발송 이력을 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.PushDelivery;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 중복 방지 키와 식별자로 푸시 발송 이력을 조회한다. */
public interface PushDeliveryRepository extends JpaRepository<PushDelivery, Long> {

  /**
   * 중복 방지 키로 기존 발송 이력을 조회한다.
   *
   * @param deduplicationKey 발송 중복 방지 키
   * @return 기존 발송 이력
   */
  Optional<PushDelivery> findByDeduplicationKey(String deduplicationKey);

  /**
   * 중복 방지 키에 해당하는 발송 이력이 존재하는지 확인한다.
   *
   * @param deduplicationKey 발송 중복 방지 키
   * @return 기존 발송 이력이 있으면 {@code true}
   */
  boolean existsByDeduplicationKey(String deduplicationKey);

  /**
   * 재시도 선점과 Ticket 결과 기록을 직렬화하기 위해 중복 방지 키로 쓰기 잠금 조회한다.
   *
   * @param deduplicationKey 발송 중복 방지 키
   * @return 잠긴 기존 발송 이력
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select delivery
      from PushDelivery delivery
      where delivery.deduplicationKey = :deduplicationKey
      """)
  Optional<PushDelivery> findByDeduplicationKeyForUpdate(
      @Param("deduplicationKey") String deduplicationKey);

  /**
   * Ticket 또는 Receipt 상태 변경을 직렬화하기 위해 발송 이력을 쓰기 잠금 조회한다.
   *
   * @param pushDeliveryId Push Delivery ID
   * @return 잠긴 발송 이력
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select delivery
      from PushDelivery delivery
      where delivery.id = :pushDeliveryId
      """)
  Optional<PushDelivery> findByIdForUpdate(@Param("pushDeliveryId") Long pushDeliveryId);
}
