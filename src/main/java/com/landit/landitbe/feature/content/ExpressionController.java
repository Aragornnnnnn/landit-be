// 원어민 표현 학습 API 요청을 받아 시나리오별 표현 목록을 공통 응답으로 반환한다.

package com.landit.landitbe.feature.content;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.docs.ExpressionControllerDocs;
import com.landit.landitbe.feature.content.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.service.ExpressionLearningCompletionService;
import com.landit.landitbe.feature.content.service.ExpressionQueryService;
import com.landit.landitbe.shared.response.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 원어민 표현 학습 API 요청을 받아 시나리오별 표현 목록을 공통 응답으로 반환한다. */
@RestController
@RequiredArgsConstructor
public class ExpressionController implements ExpressionControllerDocs {

  private final ExpressionQueryService expressionQueryService;
  private final ExpressionLearningCompletionService expressionLearningCompletionService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/expressions/{scenarioId}")
  public ApiResponse<List<ExpressionResponse>> getExpressions(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long scenarioId) {
    return ApiResponse.success(
        expressionQueryService.getExpressionsPerScenario(principal.userId(), scenarioId));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/expressions/{expressionId}/learning-start")
  public ApiResponse<ExpressionLearningResponse> getOneExpressionToStartLearning(
      @PathVariable Long expressionId) {
    return ApiResponse.success(expressionQueryService.getExpressionForLearning(expressionId));
  }

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/expressions/{expressionId}/practice")
  public ApiResponse<ExpressionPracticeResponse> getExtraPracticeExamples(
      @PathVariable Long expressionId) {
    return ApiResponse.success(expressionQueryService.getExtraPracticeExamples(expressionId));
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/expressions/{expressionId}/learning-finish")
  public ApiResponse<Map<String, Object>> finishLearning(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long expressionId) {
    expressionLearningCompletionService.completeLearning(principal.userId(), expressionId);
    return ApiResponse.success(Map.of());
  }
}
