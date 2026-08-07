// 사용자 프로필 엔티티를 PK와 상태 기준으로 조회한다.

package com.landit.landitbe.feature.profile.repository;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자 프로필 엔티티를 PK와 상태 기준으로 조회한다. */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  /** 특정 상태의 사용자 프로필을 PK로 조회한다. */
  Optional<UserProfile> findByIdAndStatus(Long id, UserProfileStatus status);

  /** 활성 사용자 프로필을 PK로 조회하면서 프로필 상태 변경을 직렬화한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select userProfile
            from UserProfile userProfile
            where userProfile.id = :id
              and userProfile.status = com.landit.landitbe.feature.profile.domain.UserProfileStatus.ACTIVE
      """)
  Optional<UserProfile> findActiveByIdForUpdate(@Param("id") Long id);

  /** 특정 상태의 사용자 프로필 존재 여부를 PK로 확인한다. */
  boolean existsByIdAndStatus(Long id, UserProfileStatus status);

  /**
   * 특정 상태와 역할을 가진 사용자 프로필 존재 여부를 확인한다.
   *
   * @param id 사용자 프로필 ID
   * @param status 확인할 사용자 프로필 상태
   * @param role 확인할 사용자 역할
   * @return 해당 상태와 역할을 가진 프로필이 있으면 {@code true}
   */
  boolean existsByIdAndStatusAndRole(Long id, UserProfileStatus status, UserRole role);
}
