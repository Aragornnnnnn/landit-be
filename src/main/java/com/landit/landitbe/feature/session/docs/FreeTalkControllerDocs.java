// 프리톡 주제 조회와 세션 시작 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.session.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.dto.FreeTalkMainResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 프리톡 주제 조회와 세션 시작 API의 OpenAPI 문서를 정의한다. */
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
}
