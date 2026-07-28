// 시나리오 조회 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.content.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.dto.ScenarioListResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 시나리오 조회 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Scenario", description = "시나리오 API")
public interface ScenarioControllerDocs {

  /**
   * 인증된 사용자의 카테고리별 시나리오 목록을 조회한다.
   *
   * @param principal 인증된 사용자 정보
   * @return 사용자별 일일 접근 상태가 반영된 시나리오 목록 응답
   */
  @Operation(
      summary = "시나리오 전체 조회",
      description = "카테고리별 시나리오 목록과 사용자별 일일 접근 상태, 시작 기한, 별점, 시작 메시지 미리보기를 조회한다.",
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
