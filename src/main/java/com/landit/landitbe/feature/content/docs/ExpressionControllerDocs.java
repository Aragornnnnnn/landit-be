// 원어민 표현 학습 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.content.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.dto.ExpressionLearningFinishRequest;
import com.landit.landitbe.feature.content.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.dto.ExpressionResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

/** 원어민 표현 학습 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Expression", description = "원어민 표현 학습 API")
public interface ExpressionControllerDocs {

  /**
   * 시나리오별 표현을 학습 순서대로 조회한다.
   *
   * @param principal 인증된 사용자
   * @param scenarioId 조회할 시나리오 ID
   * @return 완료 여부와 잠금 상태가 반영된 표현 목록
   */
  @Operation(
      summary = "시나리오별 원어민 표현 전체 조회",
      description = "표현 목록과 사용자별 완료 여부 및 잠금 상태를 반환한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "시나리오 없음")
  })
  ApiResponse<List<ExpressionResponse>> getExpressions(
      AuthUserPrincipal principal, Long scenarioId);

  /**
   * 선택한 표현의 학습 상세를 조회한다.
   *
   * @param principal 인증된 사용자
   * @param expressionId 학습할 표현 ID
   * @return 표현 학습 시작 상세와 학습 완료 여부
   */
  @Operation(
      summary = "원어민 표현 학습 시작",
      description = "선택한 표현의 뜻, 설명과 대표 예문에 사용자의 학습 완료 여부를 더해 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<ExpressionLearningResponse> getOneExpressionToStartLearning(
      AuthUserPrincipal principal, Long expressionId);

  /**
   * 선택한 표현의 추가 예문과 작문 문제를 조회한다.
   *
   * @param principal 인증된 사용자
   * @param expressionId 학습 중인 표현 ID
   * @return 추가 예문과 작문 문제
   */
  @Operation(
      summary = "원어민 표현 학습 추가 예문 조회",
      description = "눈으로 익히는 추가 예문 2건과 직접 푸는 작문 문제 2건(영어·한국어 각 1건)을 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<ExpressionPracticeResponse> getExtraPracticeExamples(
      AuthUserPrincipal principal, Long expressionId);

  /**
   * 선택한 표현의 학습 완료를 기록한다.
   *
   * @param principal 인증된 사용자
   * @param expressionId 완료할 표현 ID
   * @param request 프리톡 학습 맥락. 시나리오 학습이면 null
   * @return 빈 객체를 담은 성공 응답
   */
  @Operation(
      summary = "원어민 표현 학습 완료",
      description = "시나리오는 순차 잠금, 프리톡은 세션 연결 검증 후 완료를 기록한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<Map<String, Object>> finishLearning(
      AuthUserPrincipal principal, Long expressionId, ExpressionLearningFinishRequest request);
}
