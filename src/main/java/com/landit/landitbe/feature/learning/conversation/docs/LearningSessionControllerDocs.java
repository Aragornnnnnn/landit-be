// 공통 학습 세션 종료 API의 OpenAPI 계약을 정의한다.

package com.landit.landitbe.feature.learning.conversation.docs;

import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 공통 학습 세션 종료 API의 OpenAPI 계약을 정의한다. */
@Tag(name = "Session", description = "학습 세션 API")
public interface LearningSessionControllerDocs {

  /**
   * 진행 중인 학습 세션을 사용자가 중도 종료한다.
   *
   * @param principal 인증된 사용자
   * @param sessionId 종료할 학습 세션 ID
   * @return 데이터가 없는 성공 응답
   */
  @Operation(
      summary = "세션 중도 종료",
      description = "진행 중인 학습 세션을 INTERRUPTED 상태로 종료한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "종료 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "권한 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "세션 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "이미 완료됨")
  })
  ApiResponse<Void> endSession(AuthUserPrincipal principal, Long sessionId);
}
