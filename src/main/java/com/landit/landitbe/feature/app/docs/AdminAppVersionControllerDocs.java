// 관리자 앱 버전 정책 관리 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.app.docs;

import com.landit.landitbe.feature.app.dto.AdminAppVersionResponse;
import com.landit.landitbe.feature.app.dto.AdminAppVersionUpdateRequest;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

/** 관리자 앱 버전 정책 관리 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin App Version", description = "관리자 앱 버전 정책 API")
public interface AdminAppVersionControllerDocs {

  /**
   * 전체 앱 버전 정책을 조회한다.
   *
   * @return 플랫폼 순으로 정렬된 단일 앱 버전 정책 목록
   */
  @Operation(summary = "관리자 앱 버전 정책 목록", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<List<AdminAppVersionResponse>> list();

  /**
   * 플랫폼의 단일 앱 버전 정책을 수정한다.
   *
   * @param principal 인증된 관리자 사용자
   * @param platform 수정할 앱 플랫폼
   * @param request 앱 버전 정책 수정 요청
   * @return 수정된 앱 버전 정책
   */
  @Operation(summary = "관리자 앱 버전 정책 수정", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminAppVersionResponse> update(
      AuthUserPrincipal principal,
      AppPlatform platform,
      @Valid AdminAppVersionUpdateRequest request);
}
