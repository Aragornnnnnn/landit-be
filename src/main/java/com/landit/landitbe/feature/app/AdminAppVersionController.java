// 관리자 앱 버전 정책 관리 요청을 처리한다.

package com.landit.landitbe.feature.app;

import com.landit.landitbe.feature.app.docs.AdminAppVersionControllerDocs;
import com.landit.landitbe.feature.app.dto.AdminAppVersionResponse;
import com.landit.landitbe.feature.app.dto.AdminAppVersionUpdateRequest;
import com.landit.landitbe.feature.app.service.AppVersionService;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.response.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 앱 버전 정책 관리 요청을 처리한다. */
@RestController
public class AdminAppVersionController implements AdminAppVersionControllerDocs {

  private final AppVersionService appVersionService;

  /**
   * 관리자 앱 버전 정책 Service를 주입받는다.
   *
   * @param appVersionService 앱 버전 정책 Service
   */
  public AdminAppVersionController(AppVersionService appVersionService) {
    this.appVersionService = appVersionService;
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/admin/app-versions")
  public ApiResponse<List<AdminAppVersionResponse>> list() {
    return ApiResponse.success(appVersionService.list());
  }

  /** {@inheritDoc} */
  @Override
  @PatchMapping("/api/v1/admin/app-versions/{platform}")
  public ApiResponse<AdminAppVersionResponse> update(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable AppPlatform platform,
      @RequestBody AdminAppVersionUpdateRequest request) {
    return ApiResponse.success(appVersionService.update(principal.userId(), platform, request));
  }
}
