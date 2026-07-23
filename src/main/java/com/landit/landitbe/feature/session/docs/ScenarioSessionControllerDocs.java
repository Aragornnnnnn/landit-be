// 시나리오 세션 시작 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.session.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.dto.SessionStartResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 시나리오 세션 시작 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Scenario Session", description = "시나리오 세션 API")
public interface ScenarioSessionControllerDocs {

  /**
   * 선택한 시나리오로 학습 세션을 시작한다.
   *
   * @param principal 인증된 사용자
   * @param scenarioId 시작할 시나리오 ID
   * @return 생성된 학습 세션과 첫 메시지
   */
  @Operation(
      summary = "시나리오 세션 시작",
      description = "선택한 시나리오로 SCENARIO 타입 학습 세션을 시작한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "시작 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "잠금 상태"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "시나리오 없음")
  })
  ResponseEntity<ApiResponse<SessionStartResponse>> startScenarioSession(
      AuthUserPrincipal principal, Long scenarioId);
}
