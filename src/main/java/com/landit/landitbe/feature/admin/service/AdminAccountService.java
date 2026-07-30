// 사용자 프로필의 관리자 허용 여부를 조회한다.

package com.landit.landitbe.feature.admin.service;

import com.landit.landitbe.feature.admin.repository.AdminAccountRepository;
import org.springframework.stereotype.Service;

/** 사용자 프로필의 관리자 허용 여부를 조회한다. */
@Service
public class AdminAccountService {

  private final AdminAccountRepository adminAccountRepository;

  /**
   * 관리자 허용 목록 조회 Repository를 주입받는다.
   *
   * @param adminAccountRepository 관리자 허용 목록 Repository
   */
  public AdminAccountService(AdminAccountRepository adminAccountRepository) {
    this.adminAccountRepository = adminAccountRepository;
  }

  /**
   * 사용자 프로필이 관리자 허용 목록에 있는지 반환한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 관리자 허용 목록에 있으면 {@code true}
   */
  public boolean isAdmin(Long userProfileId) {
    return userProfileId != null && adminAccountRepository.existsByUserProfileId(userProfileId);
  }
}
