// Expo Push Token 상태 관리 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.notification.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.notification.dto.ExpoPushTokenUpdateRequest;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Expo Push Token 상태 관리 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Expo Push Token", description = "Expo Push Token 상태 관리 API")
public interface ExpoPushTokenControllerDocs {

  /**
   * Expo Push Token을 등록·갱신하거나 비활성화한다.
   *
   * @param principal 인증된 사용자
   * @param request Expo Push Token 상태 변경 요청
   * @return 성공 응답
   */
  @Operation(
      summary = "Expo Push Token 상태 변경",
      description = "현재 사용자의 Expo Push Token을 등록·갱신하거나 비활성화합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "변경 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 검증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<Void> update(AuthUserPrincipal principal, @Valid ExpoPushTokenUpdateRequest request);
}
