// 사용자 Expo Push Token을 저장하고 소유자 기준으로 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.UserPushToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자 Expo Push Token을 저장하고 소유자 기준으로 조회한다. */
public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

  /**
   * Expo Push Token 값으로 저장된 Token을 조회한다.
   *
   * @param expoPushToken Expo Push Token 값
   * @return 저장된 사용자 Push Token
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select token
      from UserPushToken token
      where token.expoPushToken = :expoPushToken
      """)
  Optional<UserPushToken> findByExpoPushTokenForUpdate(
      @Param("expoPushToken") String expoPushToken);

  /**
   * 사용자 소유 Expo Push Token을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param expoPushToken Expo Push Token 값
   * @return 해당 사용자가 소유한 Push Token
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select token
      from UserPushToken token
      where token.userProfileId = :userProfileId
        and token.expoPushToken = :expoPushToken
      """)
  Optional<UserPushToken> findOwnedTokenForUpdate(
      @Param("userProfileId") Long userProfileId, @Param("expoPushToken") String expoPushToken);
}
