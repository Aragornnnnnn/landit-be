// 소셜 로그인 제공자별 사용자 식별 정보를 저장한다.

package com.landit.landitbe.feature.auth.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 소셜 로그인 제공자별 사용자 식별 정보를 저장한다. */
@Entity
@Table(name = "oauth_identity")
public class OauthIdentity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

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

  /** JPA에서 사용하는 기본 생성자다. */
  protected OauthIdentity() {}

  /**
   * 활성 OAuth 연결 정보를 생성한다.
   *
   * @param userProfileId 연결할 사용자 프로필 ID
   * @param provider 소셜 로그인 제공자
   * @param providerUserId 제공자가 발급한 사용자 식별자
   * @param providerEmail 제공자에서 받은 이메일
   */
  public OauthIdentity(
      Long userProfileId, SocialProvider provider, String providerUserId, String providerEmail) {
    this.userProfileId = userProfileId;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.providerEmail = providerEmail;
    this.status = OauthIdentityStatus.ACTIVE;
  }

  /**
   * OAuth 제공자에서 받은 최신 이메일을 저장한다.
   *
   * @param providerEmail 제공자에서 받은 최신 이메일
   */
  public void updateProviderEmail(String providerEmail) {
    if (providerEmail != null) {
      this.providerEmail = providerEmail;
    }
  }

  /** OAuth identity를 더 이상 로그인에 쓰지 않도록 연결 해제한다. */
  public void unlink() {
    this.status = OauthIdentityStatus.UNLINKED;
  }

  /**
   * 연결된 서비스 사용자 프로필 ID를 반환한다.
   *
   * @return 연결된 사용자 프로필 ID
   */
  public Long getUserProfileId() {
    return userProfileId;
  }

  /**
   * OAuth 제공자를 반환한다.
   *
   * @return OAuth 제공자
   */
  public SocialProvider getProvider() {
    return provider;
  }
}
