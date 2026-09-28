// 사용자 Expo Push Token을 저장하고 소유자 기준으로 조회한다.

package com.landit.landitbe.feature.notification.token.repository;

import com.landit.landitbe.feature.notification.token.domain.UserPushToken;
import com.landit.landitbe.feature.notification.token.domain.UserPushTokenStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자 Expo Push Token을 저장하고 소유자 기준으로 조회한다. */
public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

  /**
   * 설치 갱신과 구형 Token 정리에 필요한 모든 행을 발송 경로와 같은 ID 순서로 잠근다.
   *
   * @param userProfileId 현재 인증된 사용자 프로필 ID
   * @param installationId 현재 설치 UUID
   * @param expoPushToken 현재 Expo Token
   * @param status 정리 대상 구형 Token 상태
   * @return 설치 갱신 대상과 정리 대상 Token 목록
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select t from UserPushToken t
      where t.installationId = :installationId
        or t.expoPushToken = :expoPushToken
        or (t.userProfileId = :userProfileId
          and t.installationId is null and t.status = :status)
      order by t.id
      """)
  List<UserPushToken> findInstallationTokensForUpdate(
      @Param("userProfileId") Long userProfileId,
      @Param("installationId") UUID installationId,
      @Param("expoPushToken") String expoPushToken,
      @Param("status") UserPushTokenStatus status);

  /** 설치 식별자로 현재 Token 행을 잠근다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from UserPushToken t where t.installationId = :installationId")
  Optional<UserPushToken> findByInstallationIdForUpdate(
      @Param("installationId") UUID installationId);

  /**
   * 발송 대상 Token을 ID 순서로 잠근다.
   *
   * @param ids Token ID 목록
   * @return 현재 Token 목록
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from UserPushToken t where t.id in :ids order by t.id")
  List<UserPushToken> findAllByIdsForUpdate(@Param("ids") List<Long> ids);

  /**
   * 실패한 Expo Token의 현재 소유 행을 ID 순서로 잠근다.
   *
   * @param values Expo Token 값 목록
   * @return 현재 Token 목록
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from UserPushToken t where t.expoPushToken in :values order by t.id")
  List<UserPushToken> findAllByValuesForUpdate(@Param("values") List<String> values);

  /**
   * 발송 직전 Token 상태와 소유자를 확인하도록 식별자로 쓰기 잠금 조회한다.
   *
   * @param userPushTokenId 사용자 Push Token ID
   * @return 잠긴 사용자 Push Token
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select token
      from UserPushToken token
      where token.id = :userPushTokenId
      """)
  Optional<UserPushToken> findByIdForUpdate(@Param("userPushTokenId") Long userPushTokenId);

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

  /**
   * 여러 사용자의 지정 상태 Token을 사용자와 식별자 순서로 조회한다.
   *
   * @param userProfileIds 사용자 프로필 ID 목록
   * @param status Token 상태
   * @return 사용자별 정렬된 Token 목록
   */
  List<UserPushToken> findAllByUserProfileIdInAndStatusOrderByUserProfileIdAscIdAsc(
      List<Long> userProfileIds, UserPushTokenStatus status);
}
