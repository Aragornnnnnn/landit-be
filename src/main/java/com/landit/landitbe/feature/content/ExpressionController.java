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

  /**
   * 시나리오별 추가학습용 원어민 표현을 학습 순서대로 조회한다.
   *
   * @param principal 인증 토큰에서 추출한 현재 사용자. 완료 여부를 이 사용자 기준으로 판정한다.
   * @param scenarioId 조회할 시나리오 ID
   */
  @Override
  @GetMapping("/api/v1/expressions/{scenarioId}")
  public ApiResponse<List<ExpressionResponse>> getExpressions(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long scenarioId) {
    return ApiResponse.success(
        expressionQueryService.getExpressionsPerScenario(principal.userId(), scenarioId));
  }

  /**
   * 사용자가 선택한 표현으로 표현 학습을 시작한다. 인증과 사용자 존재 검증은 보안 규칙과 AuthTokenFilter가 처리하므로 principal 주입은 생략한다.
   *
   * @param expressionId 학습할 영어 표현 ID
   */
  @Override
  @GetMapping("/api/v1/expressions/{expressionId}/learning-start")
  public ApiResponse<ExpressionLearningResponse> getOneExpressionToStartLearning(
      @PathVariable Long expressionId) {
    return ApiResponse.success(expressionQueryService.getExpressionForLearning(expressionId));
  }

  /**
   * 대표 예문 영작을 끝낸 뒤 추가 예문 목록과 작문 문제(예문 중 랜덤 1개)를 조회한다. 인증과 사용자 존재 검증은 보안 규칙과 AuthTokenFilter가 처리하므로
   * principal 주입은 생략한다.
   *
   * @param expressionId 학습 중인 영어 표현 ID
   */
  @Override
  @GetMapping("/api/v1/expressions/{expressionId}/practice")
  public ApiResponse<ExpressionPracticeResponse> getExtraPracticeExamples(
      @PathVariable Long expressionId) {
    return ApiResponse.success(expressionQueryService.getExtraPracticeExamples(expressionId));
  }

  /**
   * 사용자가 표현 학습을 완료한다. 이미 완료한 표현이면 마지막 완료 시각만 갱신하고, 잠긴 표현이면 거절한다.
   *
   * @param principal 인증 토큰에서 추출한 현재 사용자
   * @param expressionId 학습 완료한 영어 표현 ID
   */
  @Override
  @PostMapping("/api/v1/expressions/{expressionId}/learning-finish")
  public ApiResponse<Map<String, Object>> finishLearning(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long expressionId) {
    expressionLearningCompletionService.completeLearning(principal.userId(), expressionId);
    return ApiResponse.success(Map.of());
  }
}
