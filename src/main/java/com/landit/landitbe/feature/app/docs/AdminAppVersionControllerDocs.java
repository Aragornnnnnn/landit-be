// 관리자 앱 버전 정책 관리 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.app.docs;

import com.landit.landitbe.feature.app.dto.AdminAppVersionCreateRequest;
import com.landit.landitbe.feature.app.dto.AdminAppVersionResponse;
import com.landit.landitbe.feature.app.dto.AdminAppVersionUpdateRequest;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;

/** 관리자 앱 버전 정책 관리 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin App Version", description = "관리자 앱 버전 정책 API")
public interface AdminAppVersionControllerDocs {

  /** 전체 앱 버전 정책을 조회한다. */
  @Operation(summary = "관리자 앱 버전 정책 목록", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<List<AdminAppVersionResponse>> list();

  /** 새 앱 버전 정책을 등록한다. */
  @Operation(summary = "관리자 앱 버전 정책 등록", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<AdminAppVersionResponse>> create(
      AuthUserPrincipal principal, @Valid AdminAppVersionCreateRequest request);

  /** 앱 버전 정책을 수정한다. */
  @Operation(summary = "관리자 앱 버전 정책 수정", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminAppVersionResponse> update(
      AuthUserPrincipal principal, Long appVersionId, @Valid AdminAppVersionUpdateRequest request);

  /** 같은 플랫폼의 활성 정책을 대상 앱 버전으로 전환한다. */
  @Operation(summary = "관리자 앱 버전 정책 활성화", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminAppVersionResponse> activate(AuthUserPrincipal principal, Long appVersionId);
}
