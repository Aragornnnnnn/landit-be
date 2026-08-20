// 인증 기능에 필요한 사용자 프로필 정보만 전달한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;

/**
 * 인증 기능에 필요한 사용자 프로필 정보만 전달한다.
 *
 * @param userId 사용자 ID
 * @param nickname 사용자 닉네임
 * @param email 사용자 이메일
 * @param role 사용자 역할
 * @param status 사용자 프로필 상태
 */
public record AuthProfile(
    Long userId, String nickname, String email, UserRole role, UserProfileStatus status) {

  /**
   * 사용자 프로필을 인증 기능 공개 계약으로 변환한다.
   *
   * @param userProfile 변환할 사용자 프로필
   * @return 인증 기능용 사용자 프로필
   */
  public static AuthProfile from(UserProfile userProfile) {
    return new AuthProfile(
        userProfile.getId(),
        userProfile.getNickname(),
        userProfile.getEmail(),
        userProfile.getRole(),
        userProfile.getStatus());
  }
}
