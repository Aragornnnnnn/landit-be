// 사용자 프로필 엔티티를 PK와 상태 기준으로 조회한다.

package com.landit.landitbe.feature.auth.repository;

import com.landit.landitbe.feature.auth.domain.UserProfile;
import com.landit.landitbe.feature.auth.domain.UserProfileStatus;
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

  /** 활성 사용자 프로필을 PK로 조회하면서 동시 세션 시작을 직렬화한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select userProfile
            from UserProfile userProfile
            where userProfile.id = :id
              and userProfile.status = com.landit.landitbe.feature.auth.domain.UserProfileStatus.ACTIVE
      """)
  Optional<UserProfile> findActiveByIdForUpdate(@Param("id") Long id);

  /** 특정 상태의 사용자 프로필 존재 여부를 PK로 확인한다. */
  boolean existsByIdAndStatus(Long id, UserProfileStatus status);
}
