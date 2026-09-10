// 관리자 사용자 목록 응답을 정의한다.

package com.landit.landitbe.feature.admin.dto;

import com.landit.landitbe.feature.profile.dto.AdminUserProfilePage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 관리자 사용자 목록 응답을 정의한다.
 *
 * @param items 사용자 목록 항목
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param hasNext 다음 페이지 존재 여부
 */
public record AdminUserListResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminUserListItem> items,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasNext) {

  /**
   * 프로필 목록 페이지를 관리자 API 응답으로 변환한다.
   *
   * @param profiles 사용자 프로필 목록 페이지
   * @return 관리자 사용자 목록 응답
   */
  public static AdminUserListResponse from(AdminUserProfilePage profiles) {
    List<AdminUserListItem> items = profiles.items().stream().map(AdminUserListItem::from).toList();

    return new AdminUserListResponse(items, profiles.page(), profiles.size(), profiles.hasNext());
  }
}
