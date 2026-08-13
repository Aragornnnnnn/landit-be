// 관리자 사용자 목록과 상세 조회 요청을 처리한다.

package com.landit.landitbe.feature.admin;

import com.landit.landitbe.feature.admin.docs.AdminUserControllerDocs;
import com.landit.landitbe.feature.admin.dto.AdminUserDetailResponse;
import com.landit.landitbe.feature.admin.dto.AdminUserListResponse;
import com.landit.landitbe.feature.admin.service.AdminUserQueryService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 사용자 목록과 상세 조회 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class AdminUserController implements AdminUserControllerDocs {

  private final AdminUserQueryService adminUserQueryService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/users")
  public ApiResponse<AdminUserListResponse> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    validatePage(page, size);

    return ApiResponse.success(adminUserQueryService.getUsers(page, size));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/users/{userProfileId}")
  public ApiResponse<AdminUserDetailResponse> detail(@PathVariable long userProfileId) {
    return ApiResponse.success(adminUserQueryService.getUser(userProfileId));
  }

  private static void validatePage(int page, int size) {
    if (page < 0 || size < 1 || size > 50) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
  }
}
