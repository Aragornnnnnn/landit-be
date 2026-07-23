// 서비스 만족도 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.nps.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.nps.dto.NpsSubmitRequest;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 서비스 만족도 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "NPS", description = "서비스 만족도 API")
public interface NpsControllerDocs {

  /** 인증된 사용자의 NPS 응답을 저장한다. */
  @Operation(
      summary = "NPS 제출",
      description = "서비스 전반 만족도 점수와 선택 의견을 저장한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "제출 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 요청"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<Void>> submit(AuthUserPrincipal principal, NpsSubmitRequest request);
}
