// 관리자 사용자 목록 응답을 정의한다.

package com.landit.landitbe.feature.admin.dto;

import com.landit.landitbe.feature.profile.domain.UserProfileStatus;
import com.landit.landitbe.feature.profile.domain.UserRole;
import com.landit.landitbe.feature.profile.dto.AdminUserProfile;
import com.landit.landitbe.feature.profile.dto.AdminUserProfilePage;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 사용자 목록 응답을 정의한다.
 *
 * @param items 사용자 목록 항목
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param hasNext 다음 페이지 존재 여부
 */
public record AdminUserListResponse(List<Item> items, int page, int size, boolean hasNext) {

  /**
   * 프로필 목록 페이지를 관리자 API 응답으로 변환한다.
   *
   * @param profiles 사용자 프로필 목록 페이지
   * @return 관리자 사용자 목록 응답
   */
  public static AdminUserListResponse from(AdminUserProfilePage profiles) {
    List<Item> items = profiles.items().stream().map(Item::from).toList();

    return new AdminUserListResponse(items, profiles.page(), profiles.size(), profiles.hasNext());
  }

  /**
   * 관리자 사용자 목록의 항목이다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param email 이메일
   * @param nickname 닉네임
   * @param role 사용자 역할
   * @param status 계정 상태
   * @param createdAt 가입 시각
   */
  public record Item(
      Long userProfileId,
      String email,
      String nickname,
      UserRole role,
      UserProfileStatus status,
      LocalDateTime createdAt) {

    /** 관리자 프로필 계약을 목록 항목으로 변환한다. */
    private static Item from(AdminUserProfile profile) {
      return new Item(
          profile.userProfileId(),
          profile.email(),
          profile.nickname(),
          profile.role(),
          profile.status(),
          profile.createdAt());
    }
  }
}
