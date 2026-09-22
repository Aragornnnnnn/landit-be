// 시나리오 히스토리 조회의 인증과 정렬·피드백 반환 계약을 문서화한다.

package com.landit.landitbe.feature.learning.scenario.history.docs;

import com.landit.landitbe.feature.learning.scenario.history.dto.ScenarioHistoryResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 시나리오 히스토리 조회의 OpenAPI 문서다. */
@Tag(name = "Scenario", description = "시나리오 API")
public interface ScenarioHistoryControllerDocs {

  /**
   * 로그인한 사용자의 시나리오 전체 완료 회차를 조회한다.
   *
   * @param principal 인증 사용자 정보
   * @param scenarioId 시나리오 ID
   * @return 완료 회차별 대화와 저장된 피드백
   */
  @Operation(
      summary = "시나리오 전체 완료 회차 히스토리 조회",
      description = "로그인 사용자의 완료 회차를 최신순으로 조회한다. 기록이 없으면 빈 목록이다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "시나리오 ID 형식 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<ScenarioHistoryResponse> getHistory(
      @Parameter(hidden = true) AuthUserPrincipal principal,
      @Parameter(description = "조회할 시나리오 ID", example = "1") Long scenarioId);
}
