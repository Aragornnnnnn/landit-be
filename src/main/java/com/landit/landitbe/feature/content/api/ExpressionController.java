// 원어민 표현 학습 API 요청을 받아 시나리오별 표현 목록을 공통 응답으로 반환한다.

package com.landit.landitbe.feature.content.api;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.api.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.api.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.api.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.application.CompleteExpressionLearningUseCase;
import com.landit.landitbe.feature.content.application.ExpressionQueryService;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 원어민 표현 학습 API 요청을 받아 시나리오별 표현 목록을 공통 응답으로 반환한다. */
@RequestMapping("/api/v1/expressions")
@RestController
@RequiredArgsConstructor
@Tag(name = "Expression", description = "원어민 표현 학습 API")
public class ExpressionController {

  private final ExpressionQueryService expressionQueryService;
  private final CompleteExpressionLearningUseCase completeExpressionLearningUseCase;

  /**
   * 시나리오별 추가학습용 원어민 표현을 학습 순서대로 조회한다.
   *
   * @param principal 인증 토큰에서 추출한 현재 사용자. 완료 여부를 이 사용자 기준으로 판정한다.
   * @param scenarioId 조회할 시나리오 ID
   */
  @Operation(
      summary = "시나리오별 원어민 표현 전체 조회",
      description = "시나리오별 추가학습용 원어민 표현을 학습 순서대로 조회하고, 사용자별 완료 여부와 잠금 상태를 함께 반환한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "시나리오를 찾을 수 없음")
  })
  @GetMapping("/{scenarioId}")
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
  @Operation(
      summary = "원어민 표현 학습 시작",
      description = "사용자가 선택한 표현의 상세 정보(뜻, 설명, 대표 예문 등)를 조회해 학습을 시작한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "표현을 찾을 수 없음")
  })
  @GetMapping("/{expressionId}/learning-start")
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
  @Operation(
      summary = "원어민 표현 학습 추가 예문 조회",
      description = "대표 예문 영작을 끝낸 뒤 추가 예문 목록과 서버가 랜덤으로 선택한 작문 문제를 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "표현을 찾을 수 없거나 추가 예문이 없음")
  })
  @GetMapping("/{expressionId}/practice")
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
  @Operation(
      summary = "원어민 표현 학습 완료",
      description = "표현 학습 완료를 기록한다. 이미 완료한 표현이면 마지막 완료 시각만 갱신하고, 아직 잠긴 표현이면 완료할 수 없다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "완료 처리 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "아직 잠긴 표현"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "표현을 찾을 수 없음")
  })
  @PostMapping("/{expressionId}/learning-finish")
  public ApiResponse<Map<String, Object>> finishLearning(
      @AuthenticationPrincipal AuthUserPrincipal principal, @PathVariable Long expressionId) {
    completeExpressionLearningUseCase.completeLearning(principal.userId(), expressionId);
    return ApiResponse.success(Map.of());
  }
}
