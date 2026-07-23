// refresh token 엔티티를 저장한다.

package com.landit.landitbe.feature.auth.repository;

import com.landit.landitbe.feature.auth.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Refresh token 엔티티를 저장한다. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Refresh token 해시로 토큰 소유 사용자 ID를 조회한다.
   *
   * @param tokenHash 조회할 Refresh token 해시
   * @return 토큰 소유 사용자 ID. 토큰이 없으면 빈 값
   */
  @Query(
      """
            select token.userProfileId
            from RefreshToken token
            where token.tokenHash = :tokenHash
      """)
  Optional<Long> findUserProfileIdByTokenHash(@Param("tokenHash") String tokenHash);

  /**
   * 아직 유효하고 폐기되지 않은 Refresh token을 조건부로 폐기한다.
   *
   * @param tokenHash 폐기할 Refresh token 해시
   * @param revokedAt 폐기 기준 시각
   * @return 폐기된 토큰 수. 성공하면 1, 이미 사용됐거나 만료됐으면 0
   */
  @Modifying(flushAutomatically = true)
  @Query(
      """
            update RefreshToken token
            set token.revokedAt = :revokedAt
            where token.tokenHash = :tokenHash
              and token.revokedAt is null
              and token.expiresAt > :revokedAt
      """)
  int revokeActiveByTokenHash(
      @Param("tokenHash") String tokenHash, @Param("revokedAt") LocalDateTime revokedAt);

  /**
   * 사용자의 아직 폐기되지 않은 Refresh token을 모두 폐기한다.
   *
   * @param userProfileId 토큰을 폐기할 사용자 ID
   * @param revokedAt 폐기 시각
   */
  @Modifying
  @Query(
      """
            update RefreshToken token
            set token.revokedAt = :revokedAt
            where token.userProfileId = :userProfileId
              and token.revokedAt is null
      """)
  void revokeAllActiveByUserProfileId(
      @Param("userProfileId") Long userProfileId, @Param("revokedAt") LocalDateTime revokedAt);
}
