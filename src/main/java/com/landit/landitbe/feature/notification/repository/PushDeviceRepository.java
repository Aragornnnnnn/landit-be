// 앱 설치와 Expo Push Token 기준으로 Push Device를 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.PushDevice;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** 앱 설치와 Expo Push Token 기준으로 Push Device를 조회한다. */
public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

  /**
   * 설치 상태 동기화를 직렬화하기 위해 앱 설치 ID로 쓰기 잠금 조회한다.
   *
   * @param installationId 앱 설치 ID
   * @return 잠긴 설치 정보
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select device
      from PushDevice device
      where device.installationId = :installationId
      """)
  Optional<PushDevice> findByInstallationIdForUpdate(@Param("installationId") UUID installationId);

  /**
   * 설치와 Expo Push Token의 현재 소유 행을 식별자 순서로 함께 잠근다.
   *
   * <p>서로의 Token을 교환하는 두 설치가 반대 순서로 잠금을 요청하지 않게 한다.
   *
   * @param installationId 동기화할 앱 설치 ID
   * @param expoPushToken 동기화할 Expo Push Token. 없으면 설치 행만 조회한다.
   * @return 식별자 오름차순으로 잠긴 설치와 Token 소유 행
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  @Query(
      """
      select device
      from PushDevice device
      where device.installationId = :installationId
         or (:expoPushToken is not null and device.expoPushToken = :expoPushToken)
      order by device.id
      """)
  List<PushDevice> findByInstallationIdOrExpoPushTokenForUpdate(
      @Param("installationId") UUID installationId, @Param("expoPushToken") String expoPushToken);

  /**
   * Token 연결 이전을 직렬화하기 위해 Expo Push Token으로 쓰기 잠금 조회한다.
   *
   * @param expoPushToken Expo Push Token
   * @return 잠긴 설치 정보
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  @Query(
      """
      select device
      from PushDevice device
      where device.expoPushToken = :expoPushToken
      """)
  Optional<PushDevice> findByExpoPushTokenForUpdate(@Param("expoPushToken") String expoPushToken);

  /**
   * 발송 직전 설치 상태를 확인하고 직렬화하기 위해 식별자로 쓰기 잠금 조회한다.
   *
   * @param pushDeviceId Push Device ID
   * @return 잠긴 설치 정보
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select device
      from PushDevice device
      where device.id = :pushDeviceId
      """)
  Optional<PushDevice> findByIdForUpdate(@Param("pushDeviceId") Long pushDeviceId);
}
