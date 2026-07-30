// 관리자 앱 버전 정책 관리 요청을 처리한다.

package com.landit.landitbe.feature.app;

import com.landit.landitbe.feature.app.docs.AdminAppVersionControllerDocs;
import com.landit.landitbe.feature.app.dto.AdminAppVersionCreateRequest;
import com.landit.landitbe.feature.app.dto.AdminAppVersionResponse;
import com.landit.landitbe.feature.app.dto.AdminAppVersionUpdateRequest;
import com.landit.landitbe.feature.app.service.AppVersionService;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.shared.response.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 앱 버전 정책 관리 요청을 처리한다. */
@RestController
public class AdminAppVersionController implements AdminAppVersionControllerDocs {

  private final AppVersionService appVersionService;

  /** 관리자 앱 버전 정책 Service를 주입받는다. */
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
  @PostMapping("/api/v1/admin/app-versions")
  public ResponseEntity<ApiResponse<AdminAppVersionResponse>> create(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @RequestBody AdminAppVersionCreateRequest request) {
    return ApiResponse.success(
        HttpStatus.CREATED, appVersionService.create(principal.userId(), request));
  }

  /** {@inheritDoc} */
  @Override
  @PatchMapping("/api/v1/admin/app-versions/{appVersionId}")
  public ApiResponse<AdminAppVersionResponse> update(
      @AuthenticationPrincipal AuthUserPrincipal principal,
      @PathVariable Long appVersionId,
      @RequestBody AdminAppVersionUpdateRequest request) {
    return ApiResponse.success(appVersionService.update(principal.userId(), appVersionId, request));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/admin/app-versions/{appVersionId}/activate")
  public ApiResponse<AdminAppVersionResponse> activate(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long appVersionId) {
    return ApiResponse.success(appVersionService.activate(principal.userId(), appVersionId));
  }
}
