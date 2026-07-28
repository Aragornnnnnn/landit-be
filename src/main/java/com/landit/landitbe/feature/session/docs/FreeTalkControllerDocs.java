// 프리톡 주제 조회와 세션 시작 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.session.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkExpressionLearningResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkExpressionRetryResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkTopicResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;

/** 프리톡 주제 조회와 세션 시작 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Free Talk", description = "프리톡 세션 API")
public interface FreeTalkControllerDocs {

  /** 활성 프리톡 추천 주제를 노출 순서대로 조회한다. */
  @Operation(
      summary = "프리톡 추천 주제 조회",
      description = "활성 상태의 프리톡 추천 주제를 노출 순서대로 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<List<FreeTalkTopicResponse>>> getTopics(AuthUserPrincipal principal);

  /** AI 또는 사용자가 먼저 발화하는 프리톡 세션을 시작한다. */
  @Operation(
      summary = "프리톡 세션 시작",
      description = "AI 선시작 또는 사용자 선시작 프리톡 세션을 생성한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "시작 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "주제 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "502",
        description = "AI 응답 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "AI 생성 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkSessionStartResponse>> startSession(
      AuthUserPrincipal principal, FreeTalkSessionStartRequest request);

  /** 사용자 발화를 제출해 AI 후속 메시지 또는 종료 확인 상태를 받는다. */
  @Operation(summary = "프리톡 발화 제출", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> submitMessage(
      AuthUserPrincipal principal, long sessionId, FreeTalkMessageSubmitRequest request);

  /** 종료 의사 확인 창에서 사용자가 선택한 결과를 처리한다. */
  @Operation(summary = "프리톡 종료 의사 결정", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> decideExit(
      AuthUserPrincipal principal, long sessionId, FreeTalkExitDecisionRequest request);

  /** 완료된 지난 프리톡 목록을 최신순 페이지로 조회한다. */
  @Operation(summary = "지난 프리톡 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<FreeTalkSessionListResponse>> getSessions(
      AuthUserPrincipal principal, int page, int size);

  /** 완료된 지난 프리톡의 전체 대화와 맞춤 표현 상태를 조회한다. */
  @Operation(summary = "지난 프리톡 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<FreeTalkSessionDetailResponse>> getSession(
      AuthUserPrincipal principal, long sessionId);

  /** 실패한 맞춤 표현 생성 작업을 다시 시작한다. */
  @Operation(summary = "맞춤 표현 생성 재시도", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<FreeTalkExpressionRetryResponse>> retryExpressions(
      AuthUserPrincipal principal, long sessionId);

  /** 프리톡 맞춤 표현의 학습 시작 콘텐츠와 완료 여부를 조회한다. */
  @Operation(summary = "프리톡 맞춤 표현 학습 시작", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<FreeTalkExpressionLearningResponse>> getExpressionLearning(
      AuthUserPrincipal principal, long sessionExpressionId);

  /** 프리톡 맞춤 표현의 추가 예문과 작문 문제를 조회한다. */
  @Operation(summary = "프리톡 맞춤 표현 연습 조회", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<ExpressionPracticeResponse>> getExpressionPractice(
      AuthUserPrincipal principal, long sessionExpressionId);

  /** 프리톡 맞춤 표현 학습을 완료한다. */
  @Operation(summary = "프리톡 맞춤 표현 학습 완료", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<Map<String, Object>>> finishExpressionLearning(
      AuthUserPrincipal principal, long sessionExpressionId);
}
