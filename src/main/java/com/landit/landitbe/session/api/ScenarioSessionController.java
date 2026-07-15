// 시나리오에서 학습 세션을 시작하는 API 요청을 처리한다.
package com.landit.landitbe.session.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.session.api.dto.SessionStartResponse;
import com.landit.landitbe.session.application.ScenarioSessionStartUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
@RestController
@Tag(name = "Scenario Session", description = "시나리오 세션 API")
public class ScenarioSessionController {

  private final ScenarioSessionStartUseCase scenarioSessionStartUseCase;

  /** 선택한 시나리오로 학습 세션을 시작한다. */
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
  @PostMapping("/{scenarioId}/sessions")
  public ResponseEntity<ApiResponse<SessionStartResponse>> startScenarioSession(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long scenarioId) {
    return ApiResponse.success(
        HttpStatus.CREATED,
        scenarioSessionStartUseCase.startScenarioSession(principal.userId(), scenarioId));
  }
}
