// 관리자 사용자 프로필 목록의 페이지 계약을 전달한다.

package com.landit.landitbe.feature.profile.dto;

import com.landit.landitbe.feature.profile.domain.UserProfile;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 관리자 사용자 프로필 목록의 페이지 계약을 전달한다.
 *
 * @param items 사용자 프로필 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param hasNext 다음 페이지 존재 여부
 * @param totalCount 필터 조건에 맞는 전체 사용자 수
 * @param totalPages 전체 페이지 수. 결과가 없으면 0
 */
public record AdminUserProfilePage(
    List<AdminUserProfile> items,
    int page,
    int size,
    boolean hasNext,
    long totalCount,
    int totalPages) {

  /**
   * 사용자 프로필 Page를 관리자 공개 페이지 계약으로 변환한다.
   *
   * @param profiles 사용자 프로필 조회 결과
   * @param page 현재 페이지 번호
   * @param size 페이지 크기
   * @return 관리자 사용자 프로필 목록 페이지
   */
  public static AdminUserProfilePage from(Page<UserProfile> profiles, int page, int size) {
    List<AdminUserProfile> items =
        profiles.getContent().stream().map(AdminUserProfile::from).toList();

    return new AdminUserProfilePage(
        items,
        page,
        size,
        profiles.hasNext(),
        profiles.getTotalElements(),
        profiles.getTotalPages());
  }
}
