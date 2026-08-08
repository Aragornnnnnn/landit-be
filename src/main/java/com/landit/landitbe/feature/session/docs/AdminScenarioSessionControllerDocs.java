// 관리자 시나리오 테스트 세션 시작 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.session.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.session.dto.SessionStartResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 관리자 시나리오 테스트 세션 시작 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin Scenario", description = "관리자 시나리오 테스트 API")
public interface AdminScenarioSessionControllerDocs {

  /**
   * 진행 순서와 하루 제한 없이 활성 시나리오 테스트 세션을 시작한다.
   *
   * @param principal 인증된 관리자 정보
   * @param scenarioId 시나리오 ID
   * @return 생성된 세션과 첫 메시지 응답
   */
  @Operation(
      summary = "관리자 테스트용 시나리오 세션 시작",
      description = "develop 환경에서 진행 순서와 하루 제한 없이 활성 시나리오 테스트 세션을 시작한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "시작 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "관리자 권한 없음 또는 비활성 콘텐츠"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "시나리오 없음")
  })
  ResponseEntity<ApiResponse<SessionStartResponse>> start(
      AuthUserPrincipal principal, Long scenarioId);
}
