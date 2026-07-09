// 원어민 표현 학습 API 요청을 받아 시나리오별 표현 목록을 공통 응답으로 반환한다.
package com.landit.landitbe.content.api;

import com.landit.landitbe.auth.security.AuthUserPrincipal;
import com.landit.landitbe.common.response.ApiResponse;
import com.landit.landitbe.content.api.dto.ExpressionLearningResponse;
import com.landit.landitbe.content.api.dto.ExpressionPracticeResponse;
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

    /**
     * 사용자가 선택한 표현으로 표현 학습을 시작한다.
     * 인증과 사용자 존재 검증은 보안 규칙과 AuthTokenFilter가 처리하므로 principal 주입은 생략한다.
     *
     * @param expressionId 학습할 영어 표현 ID
     */
    @GetMapping("/{expressionId}/learning-start")
    public ApiResponse<ExpressionLearningResponse> getOneExpressionToStartLearning(
            @PathVariable Long expressionId
    ) {
        return ApiResponse.success(
                expressionQueryService.getExpressionForLearning(expressionId));
    }

    /**
     * 대표 예문 영작을 끝낸 뒤 추가 예문 목록과 작문 문제(예문 중 랜덤 1개)를 조회한다.
     * 인증과 사용자 존재 검증은 보안 규칙과 AuthTokenFilter가 처리하므로 principal 주입은 생략한다.
     *
     * @param expressionId 학습 중인 영어 표현 ID
     */
    @GetMapping("/{expressionId}/practice")
    public ApiResponse<ExpressionPracticeResponse> getExtraPracticeExamples(
            @PathVariable Long expressionId
    ) {
        return ApiResponse.success(
                expressionQueryService.getExtraPracticeExamples(expressionId));
    }
}
