// 관리자 허용 목록을 사용자 프로필 식별자로 조회한다.

package com.landit.landitbe.feature.admin.repository;

import com.landit.landitbe.feature.admin.domain.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;

/** 관리자 허용 목록을 사용자 프로필 식별자로 조회한다. */
public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {

  /**
   * 사용자 프로필이 관리자 허용 목록에 있는지 확인한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 관리자 허용 목록에 있으면 {@code true}
   */
  boolean existsByUserProfileId(Long userProfileId);
}
