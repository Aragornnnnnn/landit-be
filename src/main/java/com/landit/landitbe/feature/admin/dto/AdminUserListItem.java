// 관리자 사용자 목록의 개별 항목 응답을 정의한다.

package com.landit.landitbe.feature.admin.dto;

import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.feature.profile.dto.AdminUserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 관리자 사용자 목록의 개별 항목 응답을 정의한다. */
public record AdminUserListItem(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userProfileId,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            types = {"string", "null"})
        String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserRole role,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserProfileStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt) {

  /**
   * 관리자 프로필 계약을 목록 항목으로 변환한다.
   *
   * @param profile 변환할 관리자 프로필 계약
   * @return 관리자 사용자 목록 항목
   */
  public static AdminUserListItem from(AdminUserProfile profile) {
    return new AdminUserListItem(
        profile.userProfileId(),
        profile.email(),
        profile.nickname(),
        profile.role(),
        profile.status(),
        profile.createdAt());
  }
}
