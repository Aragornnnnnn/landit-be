// 관리자 시나리오 테스트 목록 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.content.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.dto.AdminScenarioListResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 관리자 시나리오 테스트 목록 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin Scenario", description = "관리자 시나리오 테스트 API")
public interface AdminScenarioControllerDocs {

  /**
   * 활성 콘텐츠만 관리자 테스트 목록으로 조회한다.
   *
   * @param principal 인증된 관리자 정보
   * @return 관리자 테스트용 시나리오 목록 응답
   */
  @Operation(
      summary = "관리자 테스트용 시나리오 목록 조회",
      description = "develop 환경에서 활성 상태인 시나리오 콘텐츠만 " + "관리자 테스트 목록으로 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "관리자 권한 없음")
  })
  ApiResponse<AdminScenarioListResponse> list(AuthUserPrincipal principal);
}
