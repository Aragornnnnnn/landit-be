// OAuth identity 엔티티를 소셜 식별자와 사용자 프로필 기준으로 조회한다.
package com.landit.landitbe.auth.infrastructure;

import com.landit.landitbe.auth.domain.OauthIdentity;
import com.landit.landitbe.auth.domain.OauthIdentityStatus;
import com.landit.landitbe.auth.domain.SocialProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {

  /** 활성 OAuth identity를 제공자와 제공자 사용자 식별자로 조회한다. */
  Optional<OauthIdentity> findByProviderAndProviderUserIdAndStatus(
      SocialProvider provider, String providerUserId, OauthIdentityStatus status);

  /** 사용자 프로필에 연결된 특정 상태의 OAuth identity 목록을 조회한다. */
  List<OauthIdentity> findAllByUserProfileIdAndStatus(
      Long userProfileId, OauthIdentityStatus status);
}
