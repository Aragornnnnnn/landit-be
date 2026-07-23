// refresh token 엔티티를 저장한다.

package com.landit.landitbe.feature.auth.repository;

import com.landit.landitbe.feature.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Refresh token 엔티티를 저장한다. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /** Refresh token 해시로 저장된 토큰을 조회한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** 사용자의 아직 폐기되지 않은 refresh token을 모두 폐기한다. */
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
