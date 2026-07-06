// 사용자 프로필 엔티티를 PK와 상태 기준으로 조회한다.
package com.landit.landitbe.auth.infrastructure;

import com.landit.landitbe.auth.domain.UserProfile;
import com.landit.landitbe.auth.domain.UserProfileStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** 특정 상태의 사용자 프로필을 PK로 조회한다. */
    Optional<UserProfile> findByIdAndStatus(Long id, UserProfileStatus status);

    /** 특정 상태의 사용자 프로필 존재 여부를 PK로 확인한다. */
    boolean existsByIdAndStatus(Long id, UserProfileStatus status);
}
