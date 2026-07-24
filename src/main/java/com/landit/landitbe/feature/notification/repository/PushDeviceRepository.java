// 앱 설치와 Expo Push Token 기준으로 Push Device를 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.PushDevice;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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
   * Token 연결 이전을 직렬화하기 위해 Expo Push Token으로 쓰기 잠금 조회한다.
   *
   * @param expoPushToken Expo Push Token
   * @return 잠긴 설치 정보
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select device
      from PushDevice device
      where device.expoPushToken = :expoPushToken
      """)
  Optional<PushDevice> findByExpoPushTokenForUpdate(@Param("expoPushToken") String expoPushToken);
}
