// 소셜 로그인 제공자별 사용자 식별 정보를 저장한다.

package com.landit.landitbe.feature.auth.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 소셜 로그인 제공자별 사용자 식별 정보를 저장한다. */
@Entity
@Table(name = "oauth_identity")
public class OauthIdentity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_profile_id", nullable = false)
  private UserProfile userProfile;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SocialProvider provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  @Column(name = "provider_email", length = 255)
  private String providerEmail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OauthIdentityStatus status;

  /** 동작을 수행한다. */
  protected OauthIdentity() {}

  /** 동작을 수행한다. */
  public OauthIdentity(
      UserProfile userProfile,
      SocialProvider provider,
      String providerUserId,
      String providerEmail) {
    this.userProfile = userProfile;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.providerEmail = providerEmail;
    this.status = OauthIdentityStatus.ACTIVE;
  }

  /** OAuth 제공자에서 받은 최신 이메일을 저장한다. */
  public void updateProviderEmail(String providerEmail) {
    if (providerEmail != null) {
      this.providerEmail = providerEmail;
    }
  }

  /** OAuth identity를 더 이상 로그인에 쓰지 않도록 연결 해제한다. */
  public void unlink() {
    this.status = OauthIdentityStatus.UNLINKED;
  }

  /** 연결된 서비스 사용자 프로필을 반환한다. */
  public UserProfile getUserProfile() {
    return userProfile;
  }

  /** OAuth 제공자를 반환한다. */
  public SocialProvider getProvider() {
    return provider;
  }
}
