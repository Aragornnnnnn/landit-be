// 시나리오 목록 조회 API 요청을 받아 공통 응답으로 반환한다.
package com.landit.landitbe.content.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.content.api.dto.ScenarioListResponse;
import com.landit.landitbe.content.application.ScenarioQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
@RestController
@Tag(name = "Scenario", description = "시나리오 API")
public class ScenarioController {

    private final ScenarioQueryService scenarioQueryService;

    /** 인증된 사용자의 카테고리별 시나리오 목록을 조회한다. */
    @Operation(
            summary = "시나리오 전체 조회",
            description = "카테고리별 시나리오 목록과 사용자별 완료 여부, 별점, 잠금 상태, 시작 메시지 미리보기를 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ApiResponse<ScenarioListResponse> listScenarios(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(scenarioQueryService.getScenarioList(principal.userId()));
    }
}
