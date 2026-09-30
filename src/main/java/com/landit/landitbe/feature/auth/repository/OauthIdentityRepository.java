// OAuth identity 엔티티를 소셜 식별자와 사용자 프로필 기준으로 조회한다.

package com.landit.landitbe.feature.auth.repository;

import com.landit.landitbe.feature.auth.domain.OauthIdentity;
import com.landit.landitbe.feature.auth.domain.OauthIdentityStatus;
import com.landit.landitbe.feature.auth.domain.SocialProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** OAuth identity 엔티티를 소셜 식별자와 사용자 프로필 기준으로 조회한다. */
public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {

  /** 활성 OAuth identity를 제공자와 제공자 사용자 식별자로 조회한다. */
  Optional<OauthIdentity> findByProviderAndProviderUserIdAndStatus(
      SocialProvider provider, String providerUserId, OauthIdentityStatus status);

  /** 사용자 프로필에 연결된 특정 상태의 OAuth identity 목록을 조회한다. */
  List<OauthIdentity> findAllByUserProfileIdAndStatus(
      Long userProfileId, OauthIdentityStatus status);

  /**
   * 과거 연결 해제 이력까지 포함해 소셜 식별 원본을 덮어쓰고 모든 연결을 해제한다.
   *
   * <p>호출자는 먼저 프로필 잠금을 획득해야 한다. 연결 해제 행에는 제공자 식별자 유일성 제약이 적용되지 않는다.
   *
   * @param userProfileId 탈퇴 사용자 ID
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE oauth_identity
          SET provider_user_id = 'withdrawn', provider_email = NULL,
              status = 'UNLINKED', updated_at = CURRENT_TIMESTAMP
          WHERE user_profile_id = :userProfileId
          """,
      nativeQuery = true)
  void overwritePersonalData(@Param("userProfileId") Long userProfileId);

  /**
   * 소셜 식별 원본을 덮어쓴 후 Apple 이전용 식별자 사본을 제거한다.
   *
   * @param userProfileId 탈퇴 사용자 ID
   */
  @Modifying
  @Query(
      value =
          """
          DELETE FROM apple_user_migration
          WHERE oauth_identity_id IN (
              SELECT id FROM oauth_identity WHERE user_profile_id = :userProfileId
          )
          """,
      nativeQuery = true)
  void deleteAppleMigrationData(@Param("userProfileId") Long userProfileId);
}
