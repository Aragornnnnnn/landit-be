// 시나리오 조회 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.content.api.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.api.dto.ScenarioListResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 시나리오 조회 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Scenario", description = "시나리오 API")
public interface ScenarioControllerDocs {

  /** 인증된 사용자의 카테고리별 시나리오 목록을 조회한다. */
  @Operation(
      summary = "시나리오 전체 조회",
      description = "카테고리별 시나리오 목록과 사용자별 완료 여부, 별점, 잠금 상태, 시작 메시지 미리보기를 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<ScenarioListResponse> listScenarios(AuthUserPrincipal principal);
}
