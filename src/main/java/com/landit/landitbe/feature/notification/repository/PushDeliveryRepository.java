// 중복 방지 키와 식별자로 푸시 발송 이력을 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.PushDelivery;
import com.landit.landitbe.feature.notification.domain.PushDeliveryStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 중복 방지 키와 식별자로 푸시 발송 이력을 조회한다. */
public interface PushDeliveryRepository extends JpaRepository<PushDelivery, Long> {

  /**
   * 묶음의 기존 이력을 ID 순서로 잠가 다른 발송·Receipt 처리와 잠금 순서를 맞춘다.
   *
   * @param keys 중복 방지 키 목록
   * @return 잠긴 기존 이력
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from PushDelivery d where d.deduplicationKey in :keys order by d.id")
  List<PushDelivery> findAllByKeysForUpdate(@Param("keys") List<String> keys);

  /**
   * Token 잠금을 얻은 후 다른 요청이 생성한 키를 다시 확인한다.
   *
   * @param keys 중복 방지 키 목록
   * @return 이미 존재하는 키 목록
   */
  @Query("select d.deduplicationKey from PushDelivery d where d.deduplicationKey in :keys")
  List<String> findExistingKeys(@Param("keys") List<String> keys);

  /**
   * 결과를 기록할 이력을 ID 순서로 잠근다.
   *
   * @param ids 발송 이력 ID 목록
   * @return 잠긴 발송 이력
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from PushDelivery d where d.id in :ids order by d.id")
  List<PushDelivery> findAllByIdsForUpdate(@Param("ids") List<Long> ids);

  /**
   * 중복 방지 키로 기존 발송 이력을 조회한다.
   *
   * @param deduplicationKey 발송 중복 방지 키
   * @return 기존 발송 이력
   */
  Optional<PushDelivery> findByDeduplicationKey(String deduplicationKey);

  /**
   * 중복 방지 키 접두어와 상태에 맞는 발송 이력 ID를 조회한다.
   *
   * @param status 조회할 발송 상태
   * @param deduplicationKeyPrefix 발송 이력 중복 방지 키 접두어
   * @return 조건을 만족하는 발송 이력 ID 목록
   */
  @Query(
      """
      select delivery.id
      from PushDelivery delivery
      where delivery.status = :status
        and delivery.deduplicationKey like concat(:deduplicationKeyPrefix, '%')
      order by delivery.id
      """)
  List<Long> findIdsByStatusAndDeduplicationKeyPrefix(
      @Param("status") PushDeliveryStatus status,
      @Param("deduplicationKeyPrefix") String deduplicationKeyPrefix);

  /**
   * 지정한 Token에서 중복 방지 키 접두어와 상태에 맞는 발송 이력 ID를 조회한다.
   *
   * @param status 조회할 발송 상태
   * @param deduplicationKeyPrefix 발송 이력 중복 방지 키 접두어
   * @param userPushTokenIds 조회할 Token ID
   * @return 조건을 만족하는 발송 이력 ID 목록
   */
  @Query(
      """
      select delivery.id
      from PushDelivery delivery
      where delivery.status = :status
        and delivery.deduplicationKey like concat(:deduplicationKeyPrefix, '%')
        and delivery.userPushTokenId in :userPushTokenIds
      order by delivery.id
      """)
  List<Long> findIdsByStatusAndDeduplicationKeyPrefixAndTokenIds(
      @Param("status") PushDeliveryStatus status,
      @Param("deduplicationKeyPrefix") String deduplicationKeyPrefix,
      @Param("userPushTokenIds") List<Long> userPushTokenIds);

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
