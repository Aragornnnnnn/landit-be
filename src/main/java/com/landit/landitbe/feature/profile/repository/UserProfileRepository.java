// 사용자 프로필 엔티티를 PK와 상태 기준으로 조회한다.

package com.landit.landitbe.feature.profile.repository;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자 프로필 엔티티를 PK와 상태 기준으로 조회한다. */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  /**
   * 활성 여부와 저장된 푸시 동의 여부로 사용자 프로필을 페이지 조회한다.
   *
   * @param active 활성 여부. 생략하면 모든 상태
   * @param pushConsent 저장된 푸시 동의 여부. 생략하면 모든 권한 상태
   * @param pageable 페이지 요청
   * @return 가입일 최신순 사용자 프로필 목록
   */
  @Query(
      """
      select profile from UserProfile profile
      where (:active is null
        or (:active = true and profile.status = com.landit.landitbe.feature.profile.domain.UserProfileStatus.ACTIVE)
        or (:active = false and profile.status <> com.landit.landitbe.feature.profile.domain.UserProfileStatus.ACTIVE))
        and (:pushConsent is null
          or (:pushConsent = true and profile.pushPermissionStatus = com.landit.landitbe.feature.profile.domain.PushPermissionStatus.GRANTED)
          or (:pushConsent = false and profile.pushPermissionStatus <> com.landit.landitbe.feature.profile.domain.PushPermissionStatus.GRANTED))
      order by profile.createdAt desc, profile.id desc
      """)
  Page<UserProfile> findAdminUsers(
      @Param("active") Boolean active,
      @Param("pushConsent") Boolean pushConsent,
      Pageable pageable);

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

  /**
   * 상태와 무관하게 사용자 프로필을 PK로 조회하면서 구독 상태 변경을 직렬화한다.
   *
   * <p>결제 제공자 웹훅은 탈퇴한 사용자의 환불·만료 이벤트도 반영해야 하므로 활성 조건을 두지 않는다.
   *
   * @param id 사용자 프로필 ID
   * @return 쓰기 잠금으로 조회한 사용자 프로필. 없으면 빈 값
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select userProfile from UserProfile userProfile where userProfile.id = :id")
  Optional<UserProfile> findByIdForUpdate(@Param("id") Long id);

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
