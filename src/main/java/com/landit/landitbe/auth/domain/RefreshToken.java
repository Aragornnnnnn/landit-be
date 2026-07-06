// 사용자에게 발급한 refresh token의 해시와 만료 정보를 저장한다.
package com.landit.landitbe.auth.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_refresh_token_token_hash",
                columnNames = "token_hash"
        )
)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    protected RefreshToken() {
    }

    public RefreshToken(UserProfile userProfile, String tokenHash, LocalDateTime expiresAt) {
        this.userProfile = userProfile;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /** 현재 시각 기준으로 재발급에 사용할 수 있는 refresh token인지 확인한다. */
    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    /** refresh token을 더 이상 사용할 수 없도록 폐기한다. */
    public void revoke(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    /** refresh token PK를 반환한다. */
    public Long getId() {
        return id;
    }

    /** refresh token을 발급받은 사용자를 반환한다. */
    public UserProfile getUserProfile() {
        return userProfile;
    }
}
