// 사용자에게 발급한 refresh token의 해시와 만료 정보를 저장한다.

package com.landit.landitbe.feature.auth.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** 사용자에게 발급한 refresh token의 해시와 만료 정보를 저장한다. */
@Entity
@Table(
    name = "refresh_token",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_refresh_token_token_hash", columnNames = "token_hash"))
public class RefreshToken extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected RefreshToken() {}

  /**
   * 활성 Refresh token 저장 정보를 생성한다.
   *
   * @param userProfileId 토큰 소유자 ID
   * @param tokenHash 원문을 저장하지 않기 위한 토큰 해시
   * @param expiresAt 토큰 만료 시각
   */
  public RefreshToken(Long userProfileId, String tokenHash, LocalDateTime expiresAt) {
    this.userProfileId = userProfileId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  /** 현재 시각 기준으로 재발급에 사용할 수 있는 refresh token인지 확인한다. */
  public boolean isActive(LocalDateTime now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  /** Refresh token을 더 이상 사용할 수 없도록 폐기한다. */
  public void revoke(LocalDateTime revokedAt) {
    this.revokedAt = revokedAt;
  }

  /** Refresh token PK를 반환한다. */
  public Long getId() {
    return id;
  }

  /** Refresh token을 발급받은 사용자 ID를 반환한다. */
  public Long getUserProfileId() {
    return userProfileId;
  }
}
