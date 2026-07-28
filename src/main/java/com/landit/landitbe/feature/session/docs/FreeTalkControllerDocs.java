// 프리톡 주제 조회와 세션 시작 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.session.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 프리톡 주제 조회, 세션 시작, 발화 제출과 종료 결정 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Free Talk", description = "프리톡 세션 API")
public interface FreeTalkControllerDocs {

  /**
   * 활성 프리톡 추천 주제를 노출 순서대로 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 활성 추천 주제 목록
   */
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
  ResponseEntity<ApiResponse<FreeTalkMainResponse>> getTopics(AuthUserPrincipal principal);

  /**
   * AI 또는 사용자가 먼저 발화하는 프리톡 세션을 시작한다.
   *
   * @param principal 인증된 사용자
   * @param request 세션 시작 방식과 선택 주제
   * @return 생성된 프리톡 세션
   */
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

  /**
   * 사용자 발화를 제출해 AI 후속 메시지 또는 종료 확인 상태를 받는다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 프리톡 학습 세션 ID
   * @param request 사용자 발화 제출 요청
   * @return 발화 처리 결과
   */
  @Operation(
      summary = "프리톡 발화 제출",
      description = "사용자 발화를 저장하고 AI 후속 메시지, 종료 확인 또는 시간 제한 종료를 반환한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "처리 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "세션 소유자 아님"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "중복 또는 처리 중인 발화"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "AI 생성 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> submitMessage(
      AuthUserPrincipal principal, long sessionId, FreeTalkMessageSubmitRequest request);

  /**
   * 종료 의사 확인 창에서 사용자가 선택한 결과를 처리한다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 프리톡 학습 세션 ID
   * @param request 종료 또는 계속 대화 결정 요청
   * @return 종료 결정 처리 결과
   */
  @Operation(
      summary = "프리톡 종료 의사 결정",
      description = "종료를 확정하면 마무리 메시지와 함께 세션을 완료하고, 취소하면 대화를 계속한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "처리 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "세션 소유자 아님"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "종료 확인 상태 불일치"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "AI 생성 실패")
  })
  ResponseEntity<ApiResponse<FreeTalkMessageSubmitResponse>> decideExit(
      AuthUserPrincipal principal, long sessionId, FreeTalkExitDecisionRequest request);
}
