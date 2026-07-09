// 원어민 표현 학습 API 요청을 받아 시나리오별 표현 목록을 공통 응답으로 반환한다.
package com.landit.landitbe.content.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.content.api.dto.ExpressionResponse;
import com.landit.landitbe.content.application.ExpressionQueryService;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/expressions")
@RestController
@RequiredArgsConstructor
public class ExpressionController {

    private final ExpressionQueryService expressionQueryService;

    /**
     * 시나리오별 추가학습용 원어민 표현을 학습 순서대로 조회한다.
     *
     * @param principal  인증 토큰에서 추출한 현재 사용자. 완료 여부를 이 사용자 기준으로 판정한다.
     * @param scenarioId 조회할 시나리오 ID
     */
    @GetMapping("/{scenarioId}")
    public ApiResponse<List<ExpressionResponse>> getExpressions(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long scenarioId
    ) {
        return ApiResponse.success(
                expressionQueryService.getExpressionsPerScenario(principal.userId(), scenarioId));
    }
}
